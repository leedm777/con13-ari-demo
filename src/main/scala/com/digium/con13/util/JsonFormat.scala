package com.digium.con13.util

import net.liftweb.json

trait JsonFormat {
  implicit val formats = json.DefaultFormats
}
