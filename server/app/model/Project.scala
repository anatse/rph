package model

import java.sql.Timestamp
import javax.inject.{Inject, Singleton}

import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.db.NamedDatabase
import shared._
import slick.jdbc.JdbcProfile

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object Status extends Enumeration {
  type Status = Value
  val New, Approved, InWork, Done = Value
}

@Singleton
class ProjectDAO @Inject() (@NamedDatabase("estima") protected val dbConfigProvider: DatabaseConfigProvider) extends HasDatabaseConfigProvider[JdbcProfile] {
  import profile.api._

  implicit class ResultEnrichment[T](action: DBIO[Seq[T]]) {
    def exactlyOne: DBIO[T] = action.flatMap { xs =>
      xs.length match {
        case 1 => DBIO.successful(xs.head)
        case n => DBIO.failed(new RuntimeException(s"Expected 1 result, not $n"))
      }
    }
  }

  val projects = TableQuery[ProjectTable]
  val stages = TableQuery[StageTable]

  def dropAll = {
    db.run(sql"drop table projects CASCADE".asUpdate)
  }

  def create = {
    var schema = projects.schema// ++ stages.schema
    println (s"sql: ${schema.createStatements}")
    db.run(DBIO.seq(schema.create))
  }

  def findAll (pageSize: Option[Int] = None, offset: Option[Int] = None): Future[Seq[Project]] = {
    val query = (for {
      project <- projects
    } yield (project)).sortBy(_.name.asc.nullsFirst).drop(offset.getOrElse(0)).take(pageSize.getOrElse(3))

    db.run(query.result)
  }

  def findById (projectId: Long): Future[Project] = {
    var query = projects.filter(_.id === projectId)
    db.run(query.result.exactlyOne)
  }

  def findByName (name: String): Future[Project] = {
    var query = projects.filter(_.name === name)
    db.run(query.result.exactlyOne)
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
    def number = column[String]("NUM", O.Unique)
    def description = column[Option[String]]("DESCR")
    def status = column[Int]("STATUS")
    def startDate = column[Option[Timestamp]]("START_DATE")
    def endDate = column[Option[Timestamp]]("END_DATE")

    def * = (id.?, number, name, description, status, startDate, endDate) <> (Project.tupled, Project.unapply _)
  }

  class StageTable(tag: Tag) extends Table[(Stage)](tag, "projects") {
    def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
    def projectId = column[Long]("PRJ_ID")
    def project = foreignKey("SUP_FK", projectId, projects)(_.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)
    def name = column[String]("NAME")
    def description = column[Option[String]]("DESCR")
    def status = column[Int]("STATUS")
    def startDate = column[Option[Timestamp]]("START_DATE")
    def endDate = column[Option[Timestamp]]("END_DATE")

    def * = (id.?, projectId, name, description, status, startDate, endDate) <> (Stage.tupled, Stage.unapply _)
  }

}