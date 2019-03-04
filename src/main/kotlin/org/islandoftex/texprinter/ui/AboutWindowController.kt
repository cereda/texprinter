/******************************************************************************
 * Copyright 2012-2018 Paulo Roberto Massa Cereda and Ben Frank               *
 *                                                                            *
 * Redistribution and use in source and binary forms, with or                 *
 *  without modification, are permitted provided that the following           *
 *  conditions are met:                                                       *
 *                                                                            *
 * 1. Redistributions of source code must retain the above copyright          *
 * notice, this list of conditions and the following disclaimer.              *
 *                                                                            *
 * 2. Redistributions in binary form must reproduce the above copyright       *
 * notice, this list of conditions and the following disclaimer in the        *
 * documentation and/or other materials provided with the distribution.       *
 *                                                                            *
 * 3. Neither the name of the copyright holder nor the names of it            *
 *  contributors may be used to endorse or promote products derived           *
 *  from this software without specific prior written permission.             *
 *                                                                            *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS       *
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT         *
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS         *
 *  FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE            *
 *  COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,      *
 *  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,      *
 *  BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS     *
 *  OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND    *
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR     *
 *  TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE    *
 *  USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.  *
 ******************************************************************************/
package org.islandoftex.texprinter.ui

import javafx.scene.Node
import javafx.scene.text.Font
import javafx.scene.text.Text
import javafx.scene.text.TextFlow
import tornadofx.Controller
import tornadofx.observableList

/**
 * Provides the JavaFX controller for the about/changelog window.
 *
 * @author Ben Frank
 * @version 3.0
 * @since 3.0
 */
class AboutWindowController : Controller() {
  val changelogItems = observableList<Node>()
  init {
    val changeLogMD = javaClass
        .getResource("/org/islandoftex/texprinter/config/changelog.md")
        .readText()
    changeLogMD.split("\n").forEach {
      if (it.startsWith("#")) {
        changelogItems.add(TextFlow().apply {
          children.add(Text(it.replace("#", "").trim()).apply {
            font = Font.font(18.0)
            underlineProperty().set(true)
          })
        })
      } else if (it.isNotBlank()) {
        changelogItems.add(TextFlow().apply {
          children.add(Text(it.replace("*", "").trim()))
        })
      }
    }
  }
}