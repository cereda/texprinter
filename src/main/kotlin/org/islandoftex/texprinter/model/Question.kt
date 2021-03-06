// SPDX-License-Identifier: BSD-3-Clause

package org.islandoftex.texprinter.model

import org.islandoftex.texprinter.TeXPrinter
import org.islandoftex.texprinter.utils.AppUtils
import org.islandoftex.texprinter.utils.PostComparator
import javafx.application.Platform
import javafx.scene.control.Alert
import javafx.scene.control.ButtonType
import mu.KotlinLogging
import org.islandoftex.texprinter.AppMain
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.io.IOException
import java.util.*

/**
 * Provides a simple POJO to handle question. Well, not so simple, but it
 * aims at encapsulating the logic in it.
 *
 * @author Paulo Roberto Massa Cereda
 * @version 3.0
 * @since 1.0
 */
// TODO: check handling of community-wiki posts, should the revisions appear?
class Question
/**
 * Default constructor. It fetches the online question and sets the
 * attributes defined above.
 *
 * @param questionLink The TeX.SX question link.
 */
(questionLink: String) {

  companion object {
    private val logger = KotlinLogging.logger { }
  }

  // the question
  var question: Post = Post()

  // the answers
  var answers = arrayListOf<Post>()
    get() {
      // sort the answers with the post comparator
      Collections.sort(field, PostComparator())
      // return a list of answers
      return field
    }

  init {
    // lets try to fetch data
    try {
      // log message
      logger.info { "Fetching the following question link: $questionLink" }
      // fetch the question
      val doc = Jsoup.connect(questionLink).get()

      // new post to act as the question
      val q = Post()

      // get the question title
      val questionTitle = doc.select("div#question-header").first()
          .select("h1").first().select("a").first()
      // log message
      logger.info { "Setting the question title." }
      // set the title
      q.title = questionTitle.text()

      // trying to get the question date
      val questionDate = try {
        // handles this possibility
        doc.select(".post-signature.owner").first()
            .select("span.relativetime").first()
      } catch (_: Exception) {
        // in case of failure, try this one instead
        doc.select(".post-signature").first()
            .select("span.relativetime").first()
      }
      // log message
      logger.info { "Setting the question date." }
      // set the date
      q.date = questionDate.text()

      // get the question text
      val questionText = doc.select("div.post-text").first()
      // log message
      logger.info { "Setting the question text." }
      // set the question text
      q.text = questionText.html()

      // get the question votes
      val questionVote = doc.select("div#question").first()
          .select("div.js-voting-container").first()
          .select("div.js-vote-count").first()
      // log message
      logger.info { "Setting the question votes." }
      // set the votes
      q.votes = Integer.parseInt(questionVote.text())

      // get the question comments
      val questionCommentElements = doc.select("div.comments").first()
          .select("li.comment")
      // create an array for comments
      val questionComments = ArrayList<Comment>()
      // if there are comments
      if (questionCommentElements.isNotEmpty()) {
        // log message
        logger.info { "This question has comments, getting them." }
        // iterate through comments
        questionCommentElements.forEach { questionCommentElement ->
          // create a new comment object
          val c = Comment()
          // get the text
          c.text = questionCommentElement.select("span.comment-copy")
              .first().html()
          // get the author
          c.author = questionCommentElement.select(".comment-user")
              .first().text()
          // get the date
          c.date = questionCommentElement.select("span.comment-date")
              .first().text()

          // the comment votes
          val votes: Int = try {
            // parse the votes
            Integer.parseInt(questionCommentElement.select("span.cool")
                .first().text())
          } catch (_: Exception) {
            // an error happened, set it to zero
            0
          }
          // set the votes
          c.votes = votes

          // add to the comments array
          questionComments.add(c)
        }
      } else {
        logger.info { "This question has not been commented on." }
      }
      // log message
      logger.info { "Comments retrieved, setting them to the question." }
      // set comments
      q.comments = questionComments

      // define the author name element
      var authorName: Element
      // define the author reputation element
      var authorReputation: Element? = null
      // log message
      logger.info { "Getting the question author name and reputation." }
      // lets try
      try {
        // get the name
        authorName = doc.select(".post-signature.owner").first()
            .select("div.user-details").first()
        // get the reputation
        authorReputation = doc.select(".post-signature.owner").first()
            .select("div.user-details").first()
            .select("span.reputation-score").first()
      } catch (_: Exception) {
        // something wrong happened, trying to get the name again
        authorName = doc.select(".post-signature")[1]
            .select("div.user-details")[1]
      }

      // set the temp author name
      var authorNameStr: String
      // check if this is a special question
      authorNameStr = if (authorName.getElementsByTag("a").isNotEmpty() &&
                          authorName.getElementsByTag("a").html().contains("/>")) {
        // get the author name
        authorName.getElementsByTag("a").html()
      } else {
        // lets try again
        try {
          // get the author name
          authorName.getElementsByTag("a").first().text()
        } catch (_: Exception) {
          // another error, lets try again
          authorName.text()
        }

      }
      // check if the author name needs to be retrieved from the string
      if ("/>" in authorNameStr) {
        // get the substring
        authorNameStr = authorNameStr.substring(authorNameStr.indexOf("/>") + 2)
      }

      // log message
      logger.info { "Creating a new user." }
      // a new user is created
      val u = User()

      // log message
      logger.info { "Setting the user name." }
      // set the user name
      u.name = authorNameStr

      // temp string for reputation
      val authorReputationStr: String
      // log message
      logger.info { "Checking user reputation." }
      // check if it is a normal question
      if (doc.select("div#question").select("span.community-wiki").isEmpty()) {
        // check if it is a migrated question
        authorReputationStr = if (authorName.getElementsByTag("a").isEmpty()) {
          // log message
          logger.info { "This is a migrated question." }
          // set the reputation
          "Migrated question"
        } else {
          // log message
          logger.info { "This is a normal question." }
          // normal question
          try {
            authorReputation!!.text()
          } catch (_: Exception) {
            ""
          }
        }
      } else {
        // log message
        logger.info { "This is a community wiki question." }
        // set the reputation
        authorReputationStr = "Community Wiki"
      }
      // log message
      logger.info { "Setting the user reputation." }
      // set the reputation
      u.reputation = authorReputationStr

      // log message
      logger.info { "Adding the user to the question." }
      // add the user to the question
      q.user = u

      // set the class variable
      this.question = q
      // create a new array
      this.answers = ArrayList()

      // log message
      logger.info { "Getting the answers." }
      // fetching the answers block
      val answersBlock = doc.select("div.answer")
      // check if there are answers
      if (answersBlock.isNotEmpty()) {
        // log message
        logger.info { "Answers found, retrieving them." }
        // get the authors block
        val answerAuthorsBlock = answersBlock.select("div.fw-wrap")

        // counter for the loop
        var counter = 0
        // iterate now
        answerAuthorsBlock.forEach { currentAnswerAuthor ->
          // log message
          logger.info { "Getting answer ${counter + 1}." }
          // set new post
          val a = Post()
          // set new user
          val ua = User()

          // the temp author name
          var answerAuthorNameStr: String
          // check if it is a valid entry
          answerAuthorNameStr = if (currentAnswerAuthor.select("div.user-details")
                  .first().getElementsByTag("a").isEmpty()) {
            // set the value (it is community wiki and the author is in the second tag)
            currentAnswerAuthor.select("div.user-details").last()
                .getElementsByTag("a").last().html()
          } else {
            // try another approach (it is not community wiki)
            currentAnswerAuthor.select("div.user-details").last()
                .getElementsByTag("a").first().html()
          }
          // check if user name has to be trimmed
          if ("/>" in answerAuthorNameStr) {
            // get the substring
            answerAuthorNameStr = answerAuthorNameStr
                .substring(answerAuthorNameStr.indexOf("/>") + 2)
          }
          // log message
          logger.info { "Setting the author name for answer ${counter + 1}." }
          // set the author
          ua.name = answerAuthorNameStr

          // check if it has reputation
          if (currentAnswerAuthor.select("div.user-details")
                  .last().select("span.reputation-score").isEmpty()) {
            // it is a community wiki
            if (currentAnswerAuthor.select("span.community-wiki").isNotEmpty()) {
              // log message
              logger.info { "Answer ${counter + 1} is community wiki." }
              // set the reputation
              ua.reputation = "Community Wiki"
            } else {
              // log message
              logger.info { "Answer ${counter + 1} is a migrated answer." }
              // it is a migrated question
              ua.reputation = "Migrated answer"
            }
          } else {
            // log message
            logger.info { "Answer ${counter + 1} is a normal answer." }
            // normal answer
            ua.reputation = currentAnswerAuthor.select("div.user-details")
                .last().select("span.reputation-score").first().text()
          }

          // log message
          logger.info { "Adding user to answer ${counter + 1}." }
          // add user
          a.user = ua

          // log message
          logger.info { "Adding date for answer ${counter + 1}." }
          // add date
          a.date = currentAnswerAuthor.select("div.user-info")
              .select("span.relativetime").first().text()

          // log message
          logger.info { "Adding text for answer ${counter + 1}." }
          // get text
          val answersTexts = answersBlock.select("div.post-text")
          // add text
          a.text = answersTexts[counter].html()

          // get the votes
          val theVotes = answersBlock.select("div.js-voting-container")
          // log message
          logger.info { "Adding votes for answer ${counter + 1}." }
          // set the votes
          a.votes = theVotes[counter].select("div.js-vote-count")
              .first().text().toInt()

          // check if it is accepted
          if (!theVotes[counter].getElementsByClass("vote-accepted-on").isEmpty()) {
            // log message
            logger.info { "Answer ${counter + 1} is accepted." }
            // set accepted
            a.isAccepted = true
          }

          // create the comments array
          val currentAnswerComments = ArrayList<Comment>()
          // answers comments
          val currentAnswerCommentsElements = answersBlock[counter]
              .select("div.comments").first().select("li.comment")
          // log message
          logger.info { "Checking comments for answer ${counter + 1}." }
          // if the answer has comments
          if (currentAnswerCommentsElements.isNotEmpty()) {
            // log message
            logger.info { "Adding comments for answer ${counter + 1}." }

            // iterate
            currentAnswerCommentsElements.forEach { answerCommentElement ->
              // create a new comment
              val ca = Comment()

              // set the text
              ca.text = answerCommentElement.select("span.comment-copy")
                  .first().html()
              ca.author = try {
                // try to set the author
                answerCommentElement.select("a.comment-user")
                    .first().text()
              } catch (_: Exception) {
                // fix it
                answerCommentElement.select("span.comment-user")
                    .first().text()
              }

              // set date
              ca.date = answerCommentElement.select("span.comment-date")
                  .first().text()

              // the comment votes
              val votes: Int = try {
                // try to parse it
                Integer.parseInt(answerCommentElement.select("span.cool")
                    .first().text())
              } catch (e: Exception) {
                // set default to zero
                0
              }
              // lets try
              // set votes
              ca.votes = votes
              // set the current comment to the list of comments
              currentAnswerComments.add(ca)
            }
          } else {
            // log message
            logger.info { "No comments for answer ${counter + 1}." }
          }
          // set the comments
          a.comments = currentAnswerComments

          // set to the class variable
          this.answers.add(a)
          // increments counter
          counter++
        }
        // log message
        logger.info { "All answers added." }
      } else {
        // log message
        logger.info { "There are no answers for this question." }
      }
    } catch (ex: Exception) {
      // log message
      if (ex is IOException) {
        logger.error {
          "An IO error occurred while trying to fetch and set the question " +
          "data. Possibly a 404 page. MESSAGE: ${AppUtils.printStackTrace(ex)}"
        }
      } else {
        logger.error {
          "A generic error occurred while trying to fetch and set " +
          "the question data. MESSAGE: ${AppUtils.printStackTrace(ex)}"
        }
      }

      // show dialog
      if (!AppMain.isConsoleApplication) {
        Platform.runLater {
          Alert(Alert.AlertType.ERROR,
              "I'm sorry to tell you this, but the question ID you " +
              "provided seems to lead to a 404 page. Another possible cause is a " +
              "very unstable internet connection, so the request timed out.\n\n" +
              "Please, correct the question ID and try again.",
              ButtonType.OK).showAndWait()
        }
      }
    }

  }
}
