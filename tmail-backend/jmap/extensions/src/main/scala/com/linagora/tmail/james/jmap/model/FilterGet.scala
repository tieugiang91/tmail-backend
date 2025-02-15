package com.linagora.tmail.james.jmap.model

import com.google.common.collect.ImmutableList
import org.apache.james.jmap.api.filtering.{Version, Rule => JavaRule}
import org.apache.james.jmap.api.model.{State, TypeName}
import org.apache.james.jmap.core.AccountId
import org.apache.james.jmap.core.Id.Id
import org.apache.james.jmap.mail.Name
import org.apache.james.jmap.method.WithAccountId
import org.apache.james.mailbox.model.MailboxId

import scala.util.Try

case class FilterGetRequest(accountId: AccountId,
                            ids: Option[FilterGetIds]) extends WithAccountId

case class FilterGetResponse(accountId: AccountId,
                             state: FilterState,
                             list: List[Filter],
                             notFound: FilterGetNotFound)

case class FilterGetIds(value: List[String])

case class Condition(field: Field, comparator: Comparator, value: String)

case class Field(string: String) extends AnyVal

case class Comparator(string: String) extends AnyVal

case class AppendIn(mailboxIds: List[MailboxId])

case class Action(appendIn: AppendIn)

case class Rule(name: Name, condition: Condition, action: Action)

case class Filter(id: Id, rules: List[Rule])

case class FilterWithVersion(filter: Filter, version: Version)

case class FilterState(int: Int) extends State {
  override def serialize: String = int.toString
}

case class FilterGetNotFound(value: List[String]) {
  def merge(other: FilterGetNotFound): FilterGetNotFound = FilterGetNotFound(this.value ++ other.value)
}

case object FilterTypeName extends TypeName {
  override val asString: String = "Filter"

  override def parse(string: String): Option[TypeName] = string match {
    case FilterTypeName.asString => Some(FilterTypeName)
    case _ => None
  }

  override def parseState(string: String): Either[IllegalArgumentException, FilterState] = FilterState.parse(string)
}
object Rule {
  def fromJava(rule: JavaRule, mailboxIdFactory: MailboxId.Factory): Rule =
    Rule(Name(rule.getName),
      Condition(Field(rule.getCondition.getField.asString()), Comparator(rule.getCondition.getComparator.asString()), rule.getCondition.getValue),
      Action(AppendIn(AppendIn.fromMailboxIds(rule.getAction.getAppendInMailboxes.getMailboxIds, mailboxIdFactory))))
}

object AppendIn {
  def fromMailboxIds(mailboxIds: ImmutableList[String], mailboxIdFactory: MailboxId.Factory) : List[MailboxId] = List(mailboxIdFactory.fromString(mailboxIds.get(0)))
  def convertListMailboxIdToListString(mailboxIds: List[MailboxId]) : List[String] =
    mailboxIds.map(_.serialize)
}

object FilterState {
  def toVersion(filterState: FilterState) : Version = new Version(filterState.int)
  def parse(string: String): Either[IllegalArgumentException, FilterState] = Try(Integer.parseInt(string))
    .toEither
    .map(FilterState(_))
    .left.map(new IllegalArgumentException(_))
}
