/*
 * Copyright 2015 Heiko Seeberger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.heikoseeberger.akkahttpupickle

import akka.http.scaladsl.marshalling.{ Marshaller, ToEntityMarshaller }
import akka.http.scaladsl.model.{ HttpCharsets, MediaTypes }
import akka.http.scaladsl.unmarshalling.{ FromEntityUnmarshaller, Unmarshaller }
import upickle.default.{ Reader, Writer, readJs, writeJs }
import upickle.{ Js, json }

/**
 * Automatic to and from JSON marshalling/unmarshalling using *upickle* protocol.
 */
object UpickleSupport extends UpickleSupport

/**
 * Automatic to and from JSON marshalling/unmarshalling using *upickle* protocol.
 */
trait UpickleSupport {

  implicit def upickleUnmarshallerConverter[A](reader: Reader[A]): FromEntityUnmarshaller[A] =
    upickleUnmarshaller(reader)

  /**
   * HTTP entity => `A`
   *
   * @param reader reader for `A`
   * @tparam A type to decode
   * @return unmarshaller for `A`
   */
  implicit def upickleUnmarshaller[A](implicit reader: Reader[A]): FromEntityUnmarshaller[A] =
    upickleJsValueUnmarshaller.map(readJs[A])

  /**
   * HTTP entity => JSON
   *
   * @return unmarshaller for upickle Json
   */
  implicit def upickleJsValueUnmarshaller: FromEntityUnmarshaller[Js.Value] =
    Unmarshaller.byteStringUnmarshaller
      .forContentTypes(MediaTypes.`application/json`)
      .mapWithCharset { (data, charset) =>
        val input = if (charset == HttpCharsets.`UTF-8`) data.utf8String else data.decodeString(charset.nioCharset.name)
        json.read(input)
      }

  implicit def upickleMarshallerConverter[A](writer: Writer[A])(implicit printer: Js.Value => String = json.write(_, 0)): ToEntityMarshaller[A] =
    upickleMarshaller[A](writer)

  /**
   * `A` => HTTP entity
   *
   * @param writer writer for `A`
   * @param printer pretty printer function
   * @tparam A type to encode
   * @return marshaller for any `A` value
   */
  implicit def upickleMarshaller[A](implicit writer: Writer[A], printer: Js.Value => String = json.write(_, 0)): ToEntityMarshaller[A] =
    upickleJsValueMarshaller.compose(writeJs[A])

  /**
   * JSON => HTTP entity
   *
   * @param printer pretty printer function
   * @return marshaller for any Json value
   */
  implicit def upickleJsValueMarshaller(implicit printer: Js.Value => String = json.write(_, 0)): ToEntityMarshaller[Js.Value] =
    Marshaller.StringMarshaller.wrap(MediaTypes.`application/json`)(printer)
}
