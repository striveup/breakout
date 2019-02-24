package com.neo.sk.smallBreak.model

// AUTO-GENERATED Slick data model
/** Stand-alone Slick data model for immediate use */
object SlickTables extends {
  val profile = slick.jdbc.H2Profile
} with SlickTables

/** Slick data model trait for extension, choice of backend or usage in the cake pattern. (Make sure to initialize this late.) */
trait SlickTables {
  val profile: slick.jdbc.JdbcProfile
  import profile.api._
  import slick.model.ForeignKeyAction
  // NOTE: GetResult mappers for plain SQL are only generated for tables where Slick knows how to map the types of all columns.
  import slick.jdbc.{GetResult => GR}

  /** DDL for all tables. Call .create to execute. */
  lazy val schema: profile.SchemaDescription = tGame.schema ++ tGameUser.schema ++ tUser.schema
  @deprecated("Use .schema instead of .ddl", "3.0")
  def ddl = schema

  /** Entity class storing rows of table tGame
   *  @param id Database column ID SqlType(BIGINT), AutoInc, PrimaryKey
   *  @param startTime Database column START_TIME SqlType(VARCHAR)
   *  @param endTime Database column END_TIME SqlType(VARCHAR)
   *  @param playerNum Database column PLAYER_NUM SqlType(INTEGER) */
  case class rGame(id: Long, startTime: String, endTime: String, playerNum: Int)
  /** GetResult implicit for fetching rGame objects using plain SQL queries */
  implicit def GetResultrGame(implicit e0: GR[Long], e1: GR[String], e2: GR[Int]): GR[rGame] = GR{
    prs => import prs._
    rGame.tupled((<<[Long], <<[String], <<[String], <<[Int]))
  }
  /** Table description of table GAME. Objects of this class serve as prototypes for rows in queries. */
  class tGame(_tableTag: Tag) extends profile.api.Table[rGame](_tableTag, "GAME") {
    def * = (id, startTime, endTime, playerNum) <> (rGame.tupled, rGame.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(id), Rep.Some(startTime), Rep.Some(endTime), Rep.Some(playerNum)).shaped.<>({r=>import r._; _1.map(_=> rGame.tupled((_1.get, _2.get, _3.get, _4.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column ID SqlType(BIGINT), AutoInc, PrimaryKey */
    val id: Rep[Long] = column[Long]("ID", O.AutoInc, O.PrimaryKey)
    /** Database column START_TIME SqlType(VARCHAR) */
    val startTime: Rep[String] = column[String]("START_TIME")
    /** Database column END_TIME SqlType(VARCHAR) */
    val endTime: Rep[String] = column[String]("END_TIME")
    /** Database column PLAYER_NUM SqlType(INTEGER) */
    val playerNum: Rep[Int] = column[Int]("PLAYER_NUM")
  }
  /** Collection-like TableQuery object for table tGame */
  lazy val tGame = new TableQuery(tag => new tGame(tag))

  /** Entity class storing rows of table tGameUser
   *  @param userId Database column USER_ID SqlType(BIGINT)
   *  @param gameId Database column GAME_ID SqlType(BIGINT)
   *  @param score Database column SCORE SqlType(BIGINT), Default(0) */
  case class rGameUser(userId: Long, gameId: Long, score: Long = 0L)
  /** GetResult implicit for fetching rGameUser objects using plain SQL queries */
  implicit def GetResultrGameUser(implicit e0: GR[Long]): GR[rGameUser] = GR{
    prs => import prs._
    rGameUser.tupled((<<[Long], <<[Long], <<[Long]))
  }
  /** Table description of table GAME_USER. Objects of this class serve as prototypes for rows in queries. */
  class tGameUser(_tableTag: Tag) extends profile.api.Table[rGameUser](_tableTag, "GAME_USER") {
    def * = (userId, gameId, score) <> (rGameUser.tupled, rGameUser.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(userId), Rep.Some(gameId), Rep.Some(score)).shaped.<>({r=>import r._; _1.map(_=> rGameUser.tupled((_1.get, _2.get, _3.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column USER_ID SqlType(BIGINT) */
    val userId: Rep[Long] = column[Long]("USER_ID")
    /** Database column GAME_ID SqlType(BIGINT) */
    val gameId: Rep[Long] = column[Long]("GAME_ID")
    /** Database column SCORE SqlType(BIGINT), Default(0) */
    val score: Rep[Long] = column[Long]("SCORE", O.Default(0L))
  }
  /** Collection-like TableQuery object for table tGameUser */
  lazy val tGameUser = new TableQuery(tag => new tGameUser(tag))

  /** Entity class storing rows of table tUser
   *  @param id Database column ID SqlType(BIGINT), AutoInc, PrimaryKey
   *  @param name Database column NAME SqlType(VARCHAR), Length(64,true)
   *  @param pw Database column PW SqlType(VARCHAR), Length(32,true)
   *  @param isauth Database column ISAUTH SqlType(INTEGER), Default(0)
   *  @param nickname Database column NICKNAME SqlType(VARCHAR), Length(64,true) */
  case class rUser(id: Long, name: String, pw: String, isauth: Int = 0, nickname: String)
  /** GetResult implicit for fetching rUser objects using plain SQL queries */
  implicit def GetResultrUser(implicit e0: GR[Long], e1: GR[String], e2: GR[Int]): GR[rUser] = GR{
    prs => import prs._
    rUser.tupled((<<[Long], <<[String], <<[String], <<[Int], <<[String]))
  }
  /** Table description of table USER. Objects of this class serve as prototypes for rows in queries. */
  class tUser(_tableTag: Tag) extends profile.api.Table[rUser](_tableTag, "USER") {
    def * = (id, name, pw, isauth, nickname) <> (rUser.tupled, rUser.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(id), Rep.Some(name), Rep.Some(pw), Rep.Some(isauth), Rep.Some(nickname)).shaped.<>({r=>import r._; _1.map(_=> rUser.tupled((_1.get, _2.get, _3.get, _4.get, _5.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column ID SqlType(BIGINT), AutoInc, PrimaryKey */
    val id: Rep[Long] = column[Long]("ID", O.AutoInc, O.PrimaryKey)
    /** Database column NAME SqlType(VARCHAR), Length(64,true) */
    val name: Rep[String] = column[String]("NAME", O.Length(64,varying=true))
    /** Database column PW SqlType(VARCHAR), Length(32,true) */
    val pw: Rep[String] = column[String]("PW", O.Length(32,varying=true))
    /** Database column ISAUTH SqlType(INTEGER), Default(0) */
    val isauth: Rep[Int] = column[Int]("ISAUTH", O.Default(0))
    /** Database column NICKNAME SqlType(VARCHAR), Length(64,true) */
    val nickname: Rep[String] = column[String]("NICKNAME", O.Length(64,varying=true))
  }
  /** Collection-like TableQuery object for table tUser */
  lazy val tUser = new TableQuery(tag => new tUser(tag))
}
