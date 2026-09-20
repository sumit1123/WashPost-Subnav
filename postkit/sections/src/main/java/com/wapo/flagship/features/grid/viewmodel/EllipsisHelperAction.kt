/* Copyright (c) 2024 The Washington Post. All rights reserved. */

package com.wapo.flagship.features.grid.viewmodel

sealed class EllipsisHelperAction {
    data object ActionShare : EllipsisHelperAction()
    data object ActionSaveStory : EllipsisHelperAction()
    data object ActionRemoveSavedStory : EllipsisHelperAction()
    data object ActionRead : EllipsisHelperAction()
    data object ActionGift : EllipsisHelperAction()
    data object ActionAddToPlayList : EllipsisHelperAction()
    data object ActionRemoveFromPlaylist : EllipsisHelperAction()
}
