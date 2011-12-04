package anorm.examples

import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FlatSpec
import anorm._
import Database._
import anorm.SqlParser._
import anorm.defaults._

class ModelsTests extends FlatSpec with ShouldMatchers with BeforeAndAfterEach {
  override def beforeEach() {
    Database.dropAndCreate()
  }

  behavior of "anorms and magic for yabe models"

  it should "create and retrieve a User" in {
    User.insert(User(NotAssigned, "bob@gmail.com", "secret", "Bob", false))

    val bob = User.find(
      "email={email}").on("email" -> "bob@gmail.com").first()

    bob should not be (None)
    bob.get.fullname should be("Bob")

  }

  it should "connect a User" in {

    User.create(User(NotAssigned, "bob@gmail.com", "secret", "Bob", false))

    User.connect("bob@gmail.com", "secret") should not be (None)
    User.connect("bob@gmail.com", "badpassword") should be(None)
    User.connect("tom@gmail.com", "secret") should be(None)

  }

  import java.util.{ Date }

  it should "create a Post" in {

    User.create(User(Id(1), "bob@gmail.com", "secret", "Bob", false))
    Post.create(Post(NotAssigned, "My first post", "Hello!", new Date, 1))

    Post.count().single() should be(1)

    val posts = Post.find("author_id={id}").on("id" -> 1).as(Post *)

    posts.length should be(1)

    val firstPost = posts.headOption

    firstPost should not be (None)
    firstPost.get.author_id should be(1)
    firstPost.get.title should be("My first post")
    firstPost.get.content should be("Hello!")

  }

  it should "retrieve Posts with author" in {

    User.create(User(Id(1), "bob@gmail.com", "secret", "Bob", false))
    Post.create(Post(NotAssigned, "My 1st post", "Hello world", new Date, 1))

    val posts = Post.allWithAuthor

    posts.length should be(1)

    val (post, author) = posts.head

    post.title should be("My 1st post")
    author.fullname should be("Bob")
  }

  it should "support Comments" in {

    User.create(User(Id(1), "bob@gmail.com", "secret", "Bob", false))
    Post.create(Post(Id(1), "My first post", "Hello world", new Date, 1))
    Comment.create(Comment(NotAssigned, "Jeff", "Nice post", new Date, 1))
    Comment.create(Comment(NotAssigned, "Tom", "I knew that !", new Date, 1))

    User.count().single() should be(1)
    Post.count().single() should be(1)
    Comment.count().single() should be(2)

    val Some((post, author, comments)) = Post.byIdWithAuthorAndComments(1)

    post.title should be("My first post")
    author.fullname should be("Bob")
    comments.length should be(2)
    comments(0).author should be("Jeff")
    comments(1).author should be("Tom")

  }

  it should "support Tags" in {
    User.create(User(Id(1), "nmartignole@touilleur-express.fr", "secret1", "Nicolas", false))
    val postJava = Post.create(Post(NotAssigned, "My first post", "Java and Scala : yes it rocks!", new Date, 1))
    val javaTag = Tag.create(Tag("Java"))
    val scalaTag = Tag.create(Tag("Scala"))

    Post.findTaggedWith("Java").length should be(0)
    Post.findTaggedWith("Scala").length should be(0)

    postJava.tagItWith("Java")
    postJava.tagItWith("Scala")
    Post.findTaggedWith("Java").length should be(1)
    Post.findTaggedWith("Scala").length should be(1)

    postJava.tagItWith("A new Tag that does not already exist")

    // Should reuse existing TagsForPosts
    postJava.tagItWith("Scala")
    Post.findTaggedWith("Java").length should be(1)
    Post.findTaggedWith("Scala").length should be(1)

    // Another post
    val postScala = Post.create(Post(NotAssigned, "A scala post", "Scala only", new Date, 1))
    postScala.tagItWith("Scala")
    Post.findTaggedWith("Scala").length should be(2)

    // Let's see what happens when we delete a Post
    Post.delete("where id={pid}").on("pid" -> postJava.id.get).executeUpdate()
    Post.findTaggedWith("Scala").length should be(1)
  }

  it should "retrieves a list of Post for a List of Tags" in {
    User.create(User(Id(1), "nmartignole@touilleur-express.fr", "secret1", "Nicolas", false))
    val postScala = Post.create(Post(NotAssigned, "My SCala post", "Scala for dummies", new Date, 1))
    postScala.tagItWith("Scala")

    // Create a new post, tag it with Scala and SQL
    val post3 = Post.create(Post(NotAssigned, "Third post", "A Post about Scala and NoSQL", new Date, 1))
    post3.tagItWith("Scala")
    post3.tagItWith("NoSQL")

    Post.findTaggedWith(List("Scala")).length should be(2)
    Post.findTaggedWith(List("NoSQL")).length should be(1)
    Post.findTaggedWith(List("Scala", "NoSQL")).length should be(1)
  }

  it should "returns a Tag Cloud" in {
    User.create(User(Id(1), "nmartignole@touilleur-express.fr", "secret1", "Nicolas", false))

    val postJava = Post.create(Post(NotAssigned, "My first post", "Java 7 is out!", new Date, 1))
    postJava.tagItWith("Java")
    postJava.tagItWith("JEE")

    val postScalaJava = Post.create(Post(NotAssigned, "Another post", "Java and Scala : yes it rocks!", new Date, 1))
    postScalaJava.tagItWith("Java")
    postScalaJava.tagItWith("Scala")

    val cloud: List[(String, Long)] = TagsForPosts.getCloud

    cloud.length should be(3)

    cloud.map { tagAndTotal =>
      tagAndTotal match {
        case ("Java", cpt) => cpt should be(2)
        case ("Scala", cpt) => cpt should be(1)
        case ("JEE", cpt) => cpt should be(1)
        case (_, cpt) => cpt should be(0)
      }
    }
  }
}