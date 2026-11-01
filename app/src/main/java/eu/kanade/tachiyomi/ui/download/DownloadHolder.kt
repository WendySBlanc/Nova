package eu.kanade.tachiyomi.ui.download

import android.view.View
import androidx.recyclerview.widget.ItemTouchHelper
import eu.davidea.viewholders.FlexibleViewHolder
import eu.kanade.presentation.theme.colorscheme.AndroidViewColorScheme.Companion.setColors
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.databinding.DownloadItemBinding
import eu.kanade.tachiyomi.util.view.popupMenu

/**
 * Class used to hold the data of a download.
 * All the elements from the layout file "download_item" are available in this class.
 *
 * @param view the inflated view for this holder.
 * @constructor creates a new download holder.
 */
class DownloadHolder(private val view: View, val adapter: DownloadAdapter) :
    FlexibleViewHolder(view, adapter) {

    private val binding = DownloadItemBinding.bind(view)

    init {
        setDragHandleView(binding.reorder)
        binding.menu.setOnClickListener { it.post { showPopupMenu(it) } }
    }

    private lateinit var download: Download

    /**
     * Binds this holder with the given download.
     *
     * @param download The download to bind.
     */
    fun bind(download: Download) {
        this.download = download
        // Update the chapter name.
        binding.chapterTitle.text = download.chapter.name

        // Update the manga title
        binding.mangaFullTitle.text = download.manga.title

        // KMK -->
        binding.downloadProgress.setColors(adapter.colorScheme)
        // KMK <--
        updateDownloadState()
    }

    /**
     * Updates the progress bar of the download.
     */
    fun notifyProgress() {
        updateDownloadState()
    }

    /**
     * Updates the text field of the number of downloaded pages.
     */
    fun notifyDownloadedPages() {
        updateDownloadState()
    }

    fun notifyStatus() {
        updateDownloadState()
    }

    fun isBoundTo(download: Download): Boolean {
        return this::download.isInitialized && this.download.chapter.id == download.chapter.id
    }

    private fun updateDownloadState() {
        val pages = download.pages
        val maxProgress = pages?.size?.times(100)?.coerceAtLeast(1) ?: 100
        binding.downloadProgress.max = maxProgress
        binding.downloadProgress.setProgressCompat(download.totalProgress.coerceIn(0, maxProgress), true)

        val pageProgress = pages?.let { "${download.downloadedImages}/${it.size}" }
        binding.downloadProgressText.text = when (download.status) {
            Download.State.QUEUE -> view.context.getString(R.string.download_state_queued)
            Download.State.DOWNLOADING -> pageProgress
                ?: view.context.getString(R.string.download_state_preparing)
            Download.State.DOWNLOADED -> view.context.getString(R.string.download_state_complete)
            Download.State.ERROR -> listOfNotNull(
                view.context.getString(R.string.download_state_error),
                pageProgress,
            ).joinToString(" - ")
            Download.State.NOT_DOWNLOADED -> ""
        }
    }

    override fun onItemReleased(position: Int) {
        super.onItemReleased(position)
        adapter.downloadItemListener.onItemReleased(position)
        binding.container.isDragged = false
    }

    override fun onActionStateChanged(position: Int, actionState: Int) {
        super.onActionStateChanged(position, actionState)
        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
            binding.container.isDragged = true
        }
    }

    private fun showPopupMenu(view: View) {
        view.popupMenu(
            menuRes = R.menu.download_single,
            initMenu = {
                findItem(R.id.move_to_top).isVisible = bindingAdapterPosition > 1
                findItem(R.id.move_to_bottom).isVisible =
                    bindingAdapterPosition != adapter.itemCount - 1
                findItem(R.id.retry_download).isVisible = download.status == Download.State.ERROR
            },
            onMenuItemClick = {
                adapter.downloadItemListener.onMenuItemClick(bindingAdapterPosition, this)
            },
        )
    }
}
