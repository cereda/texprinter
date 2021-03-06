// SPDX-License-Identifier: BSD-3-Clause

package org.islandoftex.texprinter.utils

import mu.KotlinLogging
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import java.io.FileOutputStream
import java.io.PrintWriter
import java.io.StringWriter
import java.net.URL
import java.util.*
import java.util.regex.Pattern

/**
 * Provides "static" functions to the application and especially the generator
 * classes. It integrates string functions as well as a download function which
 * is only relevant to the TeX generator.
 *
 * @author Paulo Roberto Massa Cereda
 * @version 3.0
 * @since 1.0
 */
object AppUtils {
  // the application logger
  private val logger = KotlinLogging.logger { }

  /**
   * Escapes HTML entities and tags to a TeX format. This method tries to
   * replace HTML code by the TeX equivalent macros.
   *
   * @param text The input text.
   * @return A new text formatted from HTML to TeX.
   */
  fun escapeHTMLtoTeX(text: String): String {
    var newText = text
        // replace headings
        .replace("<h1>", "\\subsubsubsection*{")
        .replace("</h1>", "}")
        .replace("<h2>", "\\paragraph*{")
        .replace("</h2>", "}")
        .replace("<h3>", "\\par\\emph{")
        .replace("</h3>", "} -- ")
        // replace bold tags
        .replace("<b>", "\\textbf{")
        .replace("</b>", "}")
        // replace bold tags
        .replace("<strong>", "\\textbf{")
        .replace("</strong>", "}")
        // replace italic tags
        .replace("<i>", "\\textit{")
        .replace("</i>", "}")
        // replace emphasized tags
        .replace("<em>", "\\emph{")
        .replace("</em>", "}")
        // replace paragraphs tags
        .replace("<p>", "")
        .replace("</p>", "\n\n")
        // replace ordered lists tags
        .replace("<ol>", "\\begin{enumerate}\n")
        .replace("</ol>", "\\end{enumerate}\n")
        // replace unordered lists tags
        .replace("<ul>", "\\begin{itemize}\n")
        .replace("</ul>", "\\end{itemize}\n")
        // replace item tags
        .replace("<li>", "\\item ")
        .replace("</li>", "\n")
        // replace blockquote tags
        .replace("<blockquote>", "\\begin{quotation}\n")
        .replace("</blockquote>", "\\end{quotation}\n")
        // replace code tags
        .replace("<pre><code>", "\\begin{TeXPrinterListing}\n")
        .replace("<pre class=.*\">\\s*<code>".toRegex(), "\\begin{TeXPrinterListing}\n")
        .replace("</code></pre>", "\\end{TeXPrinterListing}\n\n")
        .replace("<pre>", "\\begin{TeXPrinterListing}\n")
        .replace("</pre>", "\\end{TeXPrinterListing}\n")
        // replace inline code tags
        .replace("<code>", "\\lstinline|")
        .replace("</code>", "|")
        .replace("<font face=\"Courier\">", "\\lstinline|")
        .replace("</font>", "|")
        // replace links tags
        .replace("rel=\".*\"\\s*".toRegex(), "")
        // unify spaces
        .replace("\"\\s*/>".toRegex(), "\" />")
        .replace("\"\\s*>".toRegex(), "\">")
        // replace line breaks
        .replace("<br\\s*/>".toRegex(), "\n")
        // replace horizontal rules
        .replace("<hr\\s*/>".toRegex(), "\\\\par\\\\hrulefill\\\\par")

    // parse the text
    val docLinks = Jsoup.parse(newText)
    //docLinks.outputSettings().syntax(Document.OutputSettings.Syntax.xml)
    // get all the links
    docLinks.getElementsByTag("a").forEach {
      // replace the outer html
      newText = newText.replace(Pattern.quote(it.outerHtml()).toRegex(),
          "\\\\href{" + it.attr("href") + "}{" +
          it.text().replace("\\", "\\\\") + "}")
    }

    // create a list of images
    val images = ArrayList<ImageGroup>()
    // parse the current text
    val doc = Jsoup.parse(text)
    // fetch all the media found
    val media = doc.select("[src]")
    // for all media found (img tag)
    media.filter { it.tagName() == "img" }.forEach {
      images.add(ImageGroup(it.attr("abs:src"),
          it.attr("alt") ?: ""))
    }

    // for every image in the list of images
    images.forEach { img ->
      // lets try
      try {
        // finally, download the image to the current directory
        download(img.url, img.name)
      } catch (exception: Exception) {
        // log message
        logger.warn { "An error occurred while getting the current image. Trying to set the replacement image instead. MESSAGE: ${printStackTrace(exception)}" }
        // image could not be downloaded for any reason
        // use example-image as replacement image
        img.name = "example-image.pdf"
      }

      val caption = if (img.altText.isNotBlank() &&
                        !img.altText.startsWith("enter image") &&
                        img.altText != "alt text") "\n\\caption{${img.altText}}"
      else ""
      val figure = """
\begin{figure}
\centering
\includegraphics[scale=0.5]{${img.name}}$caption
\end{figure}
"""
      newText = newText
          .replace("<img src=\"${img.url}\" />", figure)
          .replace("<img src=\"${img.url}\" alt=\"${img.altText}\" />", figure)
    }

    // unescape all HTML entities
    newText = Parser.unescapeEntities(newText, true)

    // return new text
    return newText
  }

  /**
   * Download the file from the URL.
   *
   * @param resourceURL The resource URL.
   * @param fileName The file name.
   */
  fun download(resourceURL: String, fileName: String) {
    // log message
    logger.info { "Trying to download the file $fileName" }

    // lets try
    try {
      // open and close the connection
      URL(resourceURL).openConnection().getInputStream().use {
        FileOutputStream(fileName).use { outStream ->
          it.copyTo(outStream)
        }
      }
      // log message
      logger.info { "File $fileName downloaded successfully." }
    } catch (e: Exception) {
      // log message
      logger.error {
        "A generic error happened during the file download. " +
        "MESSAGE: ${printStackTrace(e)}"
      }
    }
  }

  /**
   * Prints the stack trace to a string. This method gets the exception
   * and prints the stack trace to a string instead of the system default
   * output.
   *
   * @param exception The exception.
   * @return The string containg the whole stack trace.
   */
  fun printStackTrace(exception: Exception): String {
    // lets try
    return try {
      // create a string writer
      val stringWriter = StringWriter()
      // create a print writer
      val printWriter = PrintWriter(stringWriter)
      // set the stack trace to the writer
      exception.printStackTrace(printWriter)
      // return the writer
      "M: " + exception.message + " S: " + stringWriter.toString()
    } catch (except: Exception) {
      // error message
      "Error in printStackTrace: " + except.message
    }
  }
}
