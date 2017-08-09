package shared

import java.sql.Timestamp

case class Project(id: Option[Long] = None, number: String, name: String, description: Option[String] = None, status: Int = 0, startDate: Option[Timestamp] = None, endDate: Option[Timestamp] = None)
case class Stage(id: Option[Long] = None, projectId: Long, name: String, description: Option[String] = None, status: Int = 0, startDate: Option[Timestamp] = None, endDate: Option[Timestamp] = None)
case class Token(token: String)

