package anorm.examples

import anorm._
import anorm.SqlParser._
import anorm.defaults._

import java.util.Date

import Database._

case class User(
  // anorm sees Id[Long] as Id[java.lang.Object] so we need to use Id[java.lang.Long]
  id: Pk[java.lang.Long],
  email: String,
  password: String,
  fullname: String,
  isAdmin: Boolean)

object User extends Dao[User] {
  def connect(email: String, password: String) = {
    User.find("email = {email} and password = {password}")
      .on("email" -> email, "password" -> password)
      .first()
  }
}

case class Post(
  id: Pk[java.lang.Long],
  title: String, content: String, postedAt: Date, author_id: Long) {

  def prevNext = {
    SQL(
      """
         (
            select *, 'next' as pos from post
            where postedAt < {date} order by postedAt desc limit 1
         )
         union
         (
            select *, 'prev' as pos from post
            where postedAt > {date} order by postedAt asc limit 1
         )
         
         order by postedAt desc
         
       """).on("date" -> postedAt).as(
        opt('pos.is("prev") ~> Post.on("")) ~ opt('pos.is("next") ~> Post.on("")) ^^ flatten)
  }

  def tagItWith(name: String) = {
    val tag = Tag.findOrCreateByName(name)
    TagsForPosts.link(tag.id.get, id.get)
  }

  def getTags: List[String] = {
    SQL("SELECT T.NAME FROM TAG t JOIN TagsForPosts tfp on tfp.tag_id=t.id join Post p on p.id=tfp.post_id where p.id={id}")
      .on("id" -> id.get)
      .as(str("tag.name") *)
  }

}

object Post extends Dao[Post] {

  def allWithAuthor: List[(Post, User)] =
    SQL("""
         select * from Post p
         join User u on p.author_id = u.id
         order by p.postedAt desc
         """).as(Post ~< User ^^ flatten *)

  def allWithAuthorAndComments: List[(Post, User, List[Comment])] =
    SQL("""
         select * from Post p
         join User u on p.author_id = u.id
         left join Comment c on c.post_id = p.id
         order by p.postedAt desc
         """).as(Post ~< User ~< Post.spanM(Comment) ^^ flatten *)

  def byIdWithAuthorAndComments(id: Long): Option[(Post, User, List[Comment])] =
    SQL("""
         select * from Post p
         join User u on p.author_id = u.id
         left join Comment c on c.post_id = p.id
         where p.id = {id}
         """).on("id" -> id).as(Post ~< User ~< Post.spanM(Comment) map flatten ?)

  def findTaggedWith(name: String): List[Post] =
    SQL("""
         select * from Post p
         join TagsForPosts tfp on p.id=tfp.post_id
         join Tag t on tfp.tag_id=t.id
         where t.name={name}
      """).on("name" -> name).as(Post *)

  def findTaggedWith(tagNames: List[String]): List[Post] = {
    println(tagNames.mkString("','"))
    SQL("select distinct(p.*) from Post p join TagsForPosts tf on p.id=tf.post_id join Tag t on tf.tag_id=t.id where t.name in ("
      + "'"
      + tagNames.mkString("','")
      + "'"
      + ") group by p.id having count(t.id) = {size}").on("size" -> tagNames.length).as(Post *)
  }
}

case class Comment(
  id: Pk[java.lang.Long],
  author: String, content: String, postedAt: Date, post_id: Long)

object Comment extends Dao[Comment] {

  def apply(post_id: Long, author: String, content: String) = {
    new Comment(NotAssigned, author, content, new Date(), post_id)
  }

}

case class Tag(id: Pk[java.lang.Long], name: String)

object Tag extends Dao[Tag] {
  def apply(name: String) = new Tag(NotAssigned, name)

  def findOrCreateByName(name: String): Tag =
    Tag.find("name={pname}").on("pname" -> name).first().getOrElse(Tag.create(Tag(name)))
}

case class TagsForPosts(id: Pk[java.lang.Long], tag_id: Long, post_id: Long)

object TagsForPosts extends Dao[TagsForPosts] {
  def apply(tag_id: Long, post_id: Long) = new TagsForPosts(NotAssigned, tag_id, post_id)

  def link(tag_id: Long, post_id: Long): Option[Long] = {
    for (
      tag <- Tag.find("id={pid}").on("pid" -> tag_id).first();
      post <- Post.find("id={pid}").on("pid" -> post_id).first();
      maybeExistingTagAndPost = TagsForPosts.find("tag_id={p1} and post_id={p2}")
        .on("p1" -> tag_id, "p2" -> post_id)
        .first();
      val newKey = {
        maybeExistingTagAndPost match {
          case Some(_) => maybeExistingTagAndPost.get.id.get
          case None => TagsForPosts.create(TagsForPosts(tag_id, post_id)).id.get
        }
      }
    ) yield newKey
  }

  def getCloud: List[(String, Long)] = {
    SQL("""
            SELECT t.name, count(p.id) as totalPosts
            FROM Post p
            JOIN TagsForPosts tfp on p.id=tfp.post_id
            JOIN Tag t ON tfp.tag_id=t.id
            GROUP BY t.name ORDER BY t.name
            """).as(str("name") ~< long("totalPosts") ^^ flatten *)
  }
}
