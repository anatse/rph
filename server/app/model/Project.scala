package model

import java.sql.Timestamp
import javax.inject.{ Inject, Singleton }

import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import play.db.NamedDatabase
import slick.jdbc.JdbcProfile

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object Status extends Enumeration {
  type Status = Value
  val New, Approved, InWork, Done = Value
}

case class Project(id: Option[Long] = None, number: String, name: String, description: String = "", status: Int = 0, startDate: Timestamp, endDate: Timestamp)

@Singleton
class ProjectDAO @Inject() (@NamedDatabase("estima") protected val dbConfigProvider: DatabaseConfigProvider) extends HasDatabaseConfigProvider[JdbcProfile] {
  import profile.api._

  val projects = TableQuery[ProjectTable]
  def create = {
    val schema = projects.schema
    db.run(DBIO.seq(schema.create))
  }

  def findAll: Future[Seq[Project]] = {
    val query = (for {
      project <- projects
    } yield (project)).sortBy(_.name)

    db.run(query.result)
  }

  def insert(prj: Project): Future[Unit] = db.run(projects += prj).map(_ => ())

  def update(id: Long, prj: Project): Future[Unit] = {
    val computerToUpdate: Project = prj.copy(Some(id))
    db.run(projects.filter(_.id === id).update(computerToUpdate)).map(_ => ())
  }

  def delete(id: Long): Future[Unit] = db.run(projects.filter(_.id === id).delete).map(_ => ())

  class ProjectTable(tag: Tag) extends Table[(Project)](tag, "projects") {
    def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
    def name = column[String]("NAME")
    def number = column[String]("NUM")
    def description = column[String]("DESCR")
    def status = column[Int]("STATUS")
    def startDate = column[Timestamp]("START_DATE")
    def endDate = column[Timestamp]("END_DATE")

    def * = (id.?, number, name, description, status, startDate, endDate) <> (Project.tupled, Project.unapply _)
  }

}