package com.wapo.flagship.features.print;

import android.widget.ProgressBar;

import com.wapo.flagship.data.Archive;
import com.wapo.flagship.data.ArchiveManager;
import com.wapo.flagship.features.print.ArchivesFragment.ArchiveItem;

import java.util.List;

import rx.Observable;

/**
 * Created by curacamalitod on 7/13/17.
 */

interface PrintSectionClickListener {

    void onSectionSelected(final ArchiveItem item);
    void downloadArchive(ArchiveItem item);
    void deleteArchive(final ArchiveItem item, final int adapterPosition);
    ArchiveManager getArchiveManager();
    List<ProgressBar> getDownloadBarList();
    Archive getPreviewArchive();
    long getPreviewDownloadId();
    boolean getPreviewDownloaded();
    public Observable<ArchivesFragment.DownloadStatus> getDownloadStatusObs(final Archive archive);

}
