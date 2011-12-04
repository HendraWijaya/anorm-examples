package anorm.examples

import anorm._
import anorm.SqlParser._
import anorm.defaults._
import java.sql.DriverManager
import java.sql.Connection

object Database {
  
  // Quick way to load the driver
  Class.forName("org.h2.Driver").newInstance
  implicit val connection = getConnection("jdbc:h2:mem:test1")
  
  def getConnection(url: String) = {
     DriverManager.getConnection(url)
  }
  
  def dropAndCreate() {
    drop()
    create()
  }
  
  def drop() {
    SQL("""
        DROP TABLE IF EXISTS TagsForPosts;  
        DROP TABLE IF EXISTS Tag;
        DROP TABLE IF EXISTS Comment;
        DROP TABLE IF EXISTS Post;
        DROP TABLE IF EXISTS User;
      """).executeUpdate()
  }
  
  def create() {
    SQL("""
        CREATE TABLE User (
           id bigint(20) NOT NULL AUTO_INCREMENT,
           email varchar(255) NOT NULL,
           password varchar(255) NOT NULL,
           fullname varchar(255) NOT NULL,
           isAdmin boolean NOT NULL,
           PRIMARY KEY (id)
        );
        
        CREATE TABLE Post (
           id bigint(20) NOT NULL AUTO_INCREMENT,
           title varchar(255) NOT NULL,
           content text NOT NULL,
           postedAt date NOT NULL,
           author_id bigint(20) NOT NULL,
           FOREIGN KEY (author_id) REFERENCES User(id),
           PRIMARY KEY (id)
        );
      
        CREATE TABLE Comment (
           id bigint(20) NOT NULL AUTO_INCREMENT,
           author varchar(255) NOT NULL,
           content text NOT NULL,
           postedAt date NOT NULL,
           post_id bigint(20) NOT NULL,
           FOREIGN KEY (post_id) REFERENCES Post(id) ON DELETE CASCADE,
           PRIMARY KEY (id)
        );
      
        CREATE TABLE Tag (
           id bigint(20) NOT NULL AUTO_INCREMENT,
           name varchar(255) NOT NULL,
           PRIMARY KEY (id)
        );
      
        CREATE TABLE TagsForPosts (
           id bigint(20) NOT NULL AUTO_INCREMENT,
           tag_id bigint(20) DEFAULT NULL,
           post_id bigint(20) DEFAULT NULL,
           PRIMARY KEY (id),
           FOREIGN KEY (post_id) REFERENCES Post(id) ON DELETE CASCADE,
           FOREIGN KEY (tag_id) REFERENCES Tag(id) ON DELETE CASCADE
        );  
      """).executeUpdate()
  }
}
