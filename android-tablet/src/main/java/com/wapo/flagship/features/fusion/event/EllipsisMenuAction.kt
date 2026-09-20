package com.wapo.flagship.features.fusion.event

import com.wapo.flagship.features.grid.model.EllipsisActionItem

sealed class EllipsisMenuAction {
    class ActionShare(
        val articleItem: EllipsisActionItem,
    ) : EllipsisMenuAction()

    class ActionSave(
        val articleItem: EllipsisActionItem,
    ) : EllipsisMenuAction()

    class ActionRemove(
        val articleItem: EllipsisActionItem,
    ) : EllipsisMenuAction()

    class ActionRead(
        val articleItem: EllipsisActionItem,
    ) : EllipsisMenuAction()

    class ActionGift(
        val articleItem: EllipsisActionItem,
    ) : EllipsisMenuAction()

    class ActionAddToPlayList(
        val articleItem: EllipsisActionItem,
    ) : EllipsisMenuAction()

    class ActionRemoveFromPlaylist(
        val articleItem: EllipsisActionItem,
    ) : EllipsisMenuAction()
}
