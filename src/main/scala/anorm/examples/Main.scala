package anorm.examples

import anorm._
import anorm.SqlParser._
import anorm.defaults._
import java.sql.DriverManager
import Database._
import java.util.Date

/**
 * This example demonstrates a standalone anorm.
 *
 * All models in this example are copied from Yabe example of play-scala module:
 * https://github.com/playframework/play-scala/blob/master/samples-and-tests/yabe/app/models.scala
 *
 * The following changes have been made to the models due to the limitation in anorm-2.0-beta:
 * - Id[T <: Anyval] is changed to use the corresponding java type because anorm sees this type as Id[java.lang.Object]
 */

object Main extends App {
  println("------- start ---------")

  Database.dropAndCreate()

  println("Total users: " + User.count().single())

  User.insert(User(Id(1), "hendra@example.com", "password", "Hendra", false))
  User.insert(User(Id(2), "wijaya@example.com", "password", "Wijaya", false))

  val users: List[User] = SQL("select * from User").as(User*)
  users.foreach(println)

  val hendra: Option[User] = User.find("fullname = {fullname}").on("fullname" -> "Hendra").first()
  println("Found Hendra: " + hendra)

  User.create(User(NotAssigned, "bob@gmail.com", "secret", "Bob", false))

  assert(User.connect("bob@gmail.com", "secret") != None)
  assert(User.connect("bob@gmail.com", "badpassword") == None)

  User.create(User(Id(4), "bob@gmail.com", "secret", "Bob", false))
  Post.create(Post(Id(1), "My first post", "Hello world", new Date, 1))
  Comment.create(Comment(NotAssigned, "Jeff", "Nice post", new Date, 1))
  Comment.create(Comment(NotAssigned, "Tom", "I knew that !", new Date, 1))
  println("Total comments: " + Comment.count().single())

  val Some((post, author, comments)) = Post.byIdWithAuthorAndComments(1)

  println("Total comments in post: " + comments.length)

  println("------- end ---------")
}

