/*
 * Copyright 2018-2020 Andrius Baruckis www.baruckis.com | kriptofolio.app
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.baruckis.kriptofolio.utilities


import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.annotation.MenuRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.content.ContextCompat
import com.baruckis.kriptofolio.R


/**
 * Helper callback class to create primary type action mode. Primary mode means a contextual action
 * bar is shown over an existing app bar or in place of one if your theme/layout does not include one.
 */
class PrimaryActionModeController : ActionMode.Callback {

    private lateinit var activity: AppCompatActivity
    private var statusBarColor: Int = 0
    private var statusBarBackground: View? = null
    private var statusBarBackgroundColor: Int = 0

    // A simple interface that listens for some action mode events.
    interface PrimaryActionModeListener {
        fun onEnterActionMode()
        fun onLeaveActionMode()
        fun onActionItemClick(item: MenuItem)
    }

    private var primaryActionModeListener: PrimaryActionModeListener? = null

    private var mode: ActionMode? = null
    @MenuRes
    private var menuResId: Int = 0
    private var title: String? = null
    private var subtitle: String? = null


    // Called after startActionMode.
    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        primaryActionModeListener?.onEnterActionMode()

        mode?.let {
            // Inflate a menu resource providing context menu items.
            mode.menuInflater.inflate(menuResId, menu)
            mode.title = title
            mode.subtitle = subtitle
            this.mode = it

            val actionModeColor = ContextCompat.getColor(activity, R.color.colorForActionModeStatusBar)

            // From Android 15 on, Window.setStatusBarColor below does nothing at all. The status
            // bar area is painted by a view of the app's own instead, see BaseActivity, so that
            // is what has to be recoloured. On Android 14 and below that view has no height and
            // is invisible, and the call below is what still does the work, so the two are safe
            // to do together - on any one device exactly one of them has an effect.
            statusBarBackground = activity.findViewById(R.id.status_bar_background)
            statusBarBackground?.let { view ->
                statusBarBackgroundColor = (view.background as? ColorDrawable)?.color
                        ?: ContextCompat.getColor(activity, R.color.colorForStatusBarBackground)
                view.setBackgroundColor(actionModeColor)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                statusBarColor = activity.window.statusBarColor
                activity.window.statusBarColor = actionModeColor
            }
        }
        return true
    }

    // Called each time the action mode is shown.
    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        return false
    }

    // Called when the action mode is finished.
    override fun onDestroyActionMode(mode: ActionMode?) {
        primaryActionModeListener?.onLeaveActionMode()

        this.mode = null

        statusBarBackground?.setBackgroundColor(statusBarBackgroundColor)
        statusBarBackground = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            activity.window.statusBarColor = statusBarColor
        }
    }

    // Called when the user selects a contextual menu item.
    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
        item?.let {
            primaryActionModeListener?.onActionItemClick(item)
        }
        return true
    }


    fun startActionMode(activity: AppCompatActivity,
                        primaryActionModeListener: PrimaryActionModeListener,
                        @MenuRes menuResId: Int,
                        title: String? = null,
                        subtitle: String? = null
    ) {
        this.menuResId = menuResId
        this.title = title
        this.subtitle = subtitle
        this.activity = activity
        this.primaryActionModeListener = primaryActionModeListener

        activity.startSupportActionMode(this)
    }

    fun finishActionMode() {
        mode?.finish()
    }

    fun isInMode(): Boolean {
        return mode != null
    }

    fun setTitle(text: String) {
        mode?.title = text
    }

}