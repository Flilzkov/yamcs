import { ChangeDetectionStrategy, ChangeDetectorRef, Component, ElementRef, OnDestroy, ViewChild } from '@angular/core';
import * as ace from 'brace';
import 'brace/mode/javascript';
import 'brace/theme/eclipse';
import 'brace/theme/twilight';
import { Subscription } from 'rxjs';
import { StorageClient } from '../../client';
import { PreferenceStore } from '../../core/services/PreferenceStore';
import { YamcsService } from '../../core/services/YamcsService';
import { Viewer } from './Viewer';

@Component({
  selector: 'app-script-viewer',
  template: `
    <div #scriptContainer class="script-container"></div>
  `,
  styles: [`
    .script-container {
      height: 100%;
    }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ScriptViewer implements Viewer, OnDestroy {

  @ViewChild('scriptContainer', { static: true })
  private scriptContainer: ElementRef;

  private editor: ace.Editor;

  private darkModeSubscription: Subscription;

  private storageClient: StorageClient;

  constructor(
    yamcs: YamcsService,
    private preferenceStore: PreferenceStore,
    private changeDetector: ChangeDetectorRef,
  ) {
    this.storageClient = yamcs.createStorageClient();
  }

  public init(objectName: string) {
    this.storageClient.getObject('_global', 'displays', objectName).then(response => {
      response.text().then(text => {
        this.scriptContainer.nativeElement.innerHTML = text;
        this.editor = ace.edit(this.scriptContainer.nativeElement);
        this.editor.setReadOnly(true);
        this.editor.getSession().setMode('ace/mode/javascript');
        this.changeDetector.detectChanges();
      });
    });

    this.applyTheme(this.preferenceStore.isDarkMode());
    this.darkModeSubscription = this.preferenceStore.darkMode$.subscribe(darkMode => {
      this.applyTheme(darkMode);
    });

    return Promise.resolve();
  }

  public hasPendingChanges() {
    return false;
  }

  private applyTheme(dark: boolean) {
    if (this.editor) {
      if (dark) {
        this.editor.setTheme('ace/theme/twilight');
      } else {
        this.editor.setTheme('ace/theme/eclipse');
      }
    }
  }

  ngOnDestroy() {
    if (this.darkModeSubscription) {
      this.darkModeSubscription.unsubscribe();
    }
  }
}
