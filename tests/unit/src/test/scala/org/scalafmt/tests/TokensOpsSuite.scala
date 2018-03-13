package org.scalafmt.tests

import utest.intercept

import scala.meta._
import scala.meta.dialects.Scala211

import org.scalafmt.internal.TokensOps._

object TokensOpsSuite extends FunSuite {

  val tokens =
    """package foo
      |
      |object Bar {
      | val baz   =   10
      |}""".stripMargin.tokenize.get

  val unknownToken = new Token.EOF(Input.None, Scala211)

  test("leadings returns all preceding tokens") {
    val Some(bar) = tokens.find {
      case Token.Ident("Bar") => true
      case _ => false
    }

    val leadings = tokens.leadings(bar)
    assert(leadings.size == 8)
    for ((a, b) <- tokens.zip(leadings.reverse)) assert(a == b)
  }

  test("leadings returns empty seq if there is no preceding tokens") {
    assert(tokens.leadings(tokens.head) == Seq())
  }

  test("leadings fails if input token does not exist") {
    intercept[NoSuchElementException] {
      tokens.leadings(unknownToken)
    }
  }

  test("trailings returns all following tokens") {
    val Some(baz) = tokens.find {
      case Token.Ident("baz") => true
      case _ => false
    }

    val trailings = tokens.trailings(baz)
    assert(trailings.size == 11)
    for ((a, b) <- tokens.reverse.zip(trailings.reverse)) assert(a == b)
  }

  test("trailings returns empty seq if there is no following tokens") {
    assert(tokens.trailings(tokens.last) == Seq())
  }

  test("trailings fails if input token does not exist") {
    intercept[NoSuchElementException] {
      tokens.trailings(unknownToken)
    }
  }

  test("find returns first token following `start` matching the predicate") {
    assert(tokens.find(tokens.head)(_.is[Token.EOF]) == Some(tokens.last))
  }

  test("find returns `start` token if it matches predicate") {
    assert(tokens.find(tokens.head)(_.is[Token.BOF]) == Some(tokens.head))
  }

  test("find returns none if token matching predicate comes before `start`") {
    assert(tokens.find(tokens.last)(_.is[Token.BOF]) == None)
  }

  test("find fails if `start` token does not exist") {
    intercept[NoSuchElementException] {
      tokens.find(unknownToken)(_.is[Token.BOF])
    }
  }

  test("prev returns the preceding token") {
    assert(tokens.prev(tokens(1)) == tokens.head)
  }

  test("prev returns self if there is no preceding token") {
    assert(tokens.prev(tokens.head) == tokens.head)
  }

  test("next returns the following token") {
    assert(tokens.next(tokens.head) == tokens(1))
  }

  test("next returns self if there is no following token") {
    assert(tokens.next(tokens.last) == tokens.last)
  }

  test("slice returns `from` if there is no more tokens in between") {
    assert(tokens.slice(tokens.head, tokens(1)) == Seq(tokens.head))
  }

  test("slice returns empty seq if `from` and `to` tokens are the same object") {
    assert(tokens.slice(tokens.head, tokens.head) == Seq())
  }

  test("slice returns empty seq if `from` comes after `to`") {
    assert(tokens.slice(tokens.last, tokens.head) == Seq())
  }

  test("slice fails if `from` token does not exist") {
    intercept[NoSuchElementException] {
      tokens.slice(tokens.head, unknownToken)
    }
  }

  test("slice fails if `to` token does not exist") {
    intercept[NoSuchElementException] {
      tokens.slice(unknownToken, tokens.last)
    }
  }

  test("slice returns tokens between `from` (inclusive) and `to`") {
    val Some(kwObject) = tokens.find(_.is[Token.KwObject])
    val Some(leftBrace) = tokens.find(_.is[Token.LeftBrace])

    val slice = tokens.slice(kwObject, leftBrace)
    assert(slice.size == 4)
    val Seq(kwObj, space1, bar, space2) = slice
    assert(kwObj == kwObject)
    assert(space1.is[Token.Space])
    assert(bar.syntax.equals("Bar"))
    assert(space2.is[Token.Space])
  }

  test("leadingSpaces returns all spaces preceding a token") {
    val Some(equals) = tokens.find(_.is[Token.Equals])

    val spaces = tokens.leadingSpaces(equals)
    assert(spaces.size == 3)
    val Seq(s1, s2, s3) = spaces
    assert(s1 == tokens.prev(equals))
    assert(s2 == tokens.prev(s1))
    assert(s3 == tokens.prev(s2))
  }

  test(
    "leadingSpaces returns an empty seq if there's no space preceding a token"
  ) {
    val Some(kwPackage) = tokens.find(_.is[Token.KwPackage])

    assert(tokens.leadingSpaces(kwPackage) == Seq())
  }

  test("trailingSpaces returns all spaces following a token") {
    val Some(equals) = tokens.find(_.is[Token.Equals])

    val spaces = tokens.trailingSpaces(equals)
    assert(spaces.size == 3)
    val Seq(s1, s2, s3) = spaces
    assert(s1 == tokens.next(equals))
    assert(s2 == tokens.next(s1))
    assert(s3 == tokens.next(s2))
  }

  test(
    "trailingSpaces returns an empty seq if there's no space following a token"
  ) {
    val Some(rightBrace) = tokens.find(_.is[Token.RightBrace])

    assert(tokens.trailingSpaces(rightBrace) == Seq())
  }
}
