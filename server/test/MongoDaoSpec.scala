import models.mongo.{MongoDao, Person}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import scala.concurrent.duration._

import scala.concurrent.Await

class MongoDaoSpec extends PlaySpec with GuiceOneAppPerTest {
  "MongoDao service" should {
    val md:MongoDao = new MongoDao()

    "connect to database" in {
      Await.result(md.createPerson(Person("красивый красный цвет", "ivanov", 20)), 10 second)

      val person = Await.result(md.findPersonByAge(20), 10 second)
      person.length must be (4)
      person(0).firstName must be ("vasya")
      person(0).lastName must be ("ivanov")
      person(0).age must be (20)
    }

    "create text index" in {
      val res = Await.result(md.createTextIndex(), 10 second)
      println(res)
      res.ok must be (true)
    }

    "search by text index" in {
      val res = Await.result(md.findByText("красно -уродливо"), 10 second)
      println(res)
      res.length must be (1)
    }
  }
}