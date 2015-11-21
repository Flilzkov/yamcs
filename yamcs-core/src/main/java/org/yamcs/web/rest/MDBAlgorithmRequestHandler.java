package org.yamcs.web.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yamcs.protobuf.Mdb.AlgorithmInfo;
import org.yamcs.protobuf.Rest.ListAlgorithmsResponse;
import org.yamcs.protobuf.SchemaMdb;
import org.yamcs.protobuf.SchemaRest;
import org.yamcs.protobuf.Yamcs.NamedObjectId;
import org.yamcs.web.rest.RestUtils.MatchResult;
import org.yamcs.web.rest.XtceToGpbAssembler.DetailLevel;
import org.yamcs.xtce.Algorithm;
import org.yamcs.xtce.XtceDb;

/**
 * Handles incoming requests related to algorithm info from the MDB
 */
public class MDBAlgorithmRequestHandler extends RestRequestHandler {
    final static Logger log = LoggerFactory.getLogger(MDBAlgorithmRequestHandler.class.getName());
    
    @Override
    public String getPath() {
        return "algorithm";
    }
    
    @Override
    public RestResponse handleRequest(RestRequest req, int pathOffset) throws RestException {
        XtceDb mdb = req.getFromContext(MDBRequestHandler.CTX_MDB);
        if (!req.hasPathSegment(pathOffset)) {
            return listAlgorithms(req, null, mdb); // root namespace
        } else {
            MatchResult<Algorithm> am = RestUtils.matchAlgorithmName(req, pathOffset);
            if (am.matches()) { // algorithm
                return getSingleAlgorithm(req, am.getRequestedId(), am.getMatch());
            } else { // namespace
                return listAlgorithmsOrError(req, pathOffset);
            }
        }
    }
    
    private RestResponse listAlgorithmsOrError(RestRequest req, int pathOffset) throws RestException {
        XtceDb mdb = req.getFromContext(MDBRequestHandler.CTX_MDB);
        MatchResult<String> nsm = RestUtils.matchXtceDbNamespace(req, pathOffset, true);
        if (nsm.matches()) {
            return listAlgorithms(req, nsm.getMatch(), mdb);
        } else {
            throw new NotFoundException(req);
        }
    }
    
    private RestResponse getSingleAlgorithm(RestRequest req, NamedObjectId id, Algorithm a) throws RestException {
        // TODO privileges
        String instanceURL = req.getApiURL() + "/mdb/" + req.getFromContext(RestRequest.CTX_INSTANCE);
        AlgorithmInfo ainfo = XtceToGpbAssembler.toAlgorithmInfo(a, instanceURL, DetailLevel.FULL);
        return new RestResponse(req, ainfo, SchemaMdb.AlgorithmInfo.WRITE);
    }

    /**
     * Sends the containers for the requested yamcs instance. If no namespace
     * is specified, assumes root namespace.
     */
    private RestResponse listAlgorithms(RestRequest req, String namespace, XtceDb mdb) throws RestException {
        String instanceURL = req.getApiURL() + "/mdb/" + req.getFromContext(RestRequest.CTX_INSTANCE);
        
        NameDescriptionSearchMatcher matcher = null;
        if (req.hasQueryParameter("q")) {
            matcher = new NameDescriptionSearchMatcher(req.getQueryParameter("q"));    
        }
        
        ListAlgorithmsResponse.Builder responseb = ListAlgorithmsResponse.newBuilder();
        if (namespace == null) {
            for (Algorithm a : mdb.getAlgorithms()) {
                if (matcher != null && !matcher.matches(a)) continue;
                responseb.addAlgorithm(XtceToGpbAssembler.toAlgorithmInfo(a, instanceURL, DetailLevel.SUMMARY));
            }
        } else {
            // TODO privileges
            for (Algorithm a : mdb.getAlgorithms()) {
                if (matcher != null && !matcher.matches(a))
                    continue;
                
                String alias = a.getAlias(namespace);
                if (alias != null) {
                    responseb.addAlgorithm(XtceToGpbAssembler.toAlgorithmInfo(a, instanceURL, DetailLevel.SUMMARY));
                }
            }
        }
        
        return new RestResponse(req, responseb.build(), SchemaRest.ListAlgorithmsResponse.WRITE);
    }
}
