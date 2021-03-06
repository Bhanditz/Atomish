import net.flaviusb.atomish._

import java.io.{File, FileInputStream, Writer, Reader, BufferedReader, StringReader, IOException, FileReader, FileWriter}
import scala.io.{BufferedSource}

import scala.collection.mutable.{Map => MMap}

var u = new PreUniverse()
var r = new PreReader()
var e = PreEvaller.eval(u) _

u.roots("read") = r.alien_read;
u.roots("eval") = AlienProxy(_.args match {
  case List(Left(arg: AtomishCode))                           => e(arg, None)
  case List(Left(arg: AtomishCode), Left(base: AtomishThing)) => e(arg, Some(base))
  case _                                                      => null // Should error
})
u.roots("print") = AlienProxy(_.args match {
  case List(Left(arg: AtomishThing)) => AtomishString(PreScalaPrinter.print(arg))
  case _                             => null // Should error
})
u.roots("print_with_forms") = AlienProxy(_.args match {
  case List(Left(arg: AtomishThing)) => AtomishString(PreScalaPrinter.print_with_forms(arg))
  case _                             => null // Should error
})

u.roots("System") = AtomishOrigin(MMap[String, AtomishThing](
  "programArguments" -> AtomishArray(Array())
))

u.roots("FileSystem") = AtomishOrigin(MMap[String, AtomishThing](
  "cwd"              -> AtomishString((new File(".")).getAbsoluteFile().getParent()),
  "exists?"          -> AlienProxy(_.args match {
    case List(Left(AtomishString(file_name))) => AtomishBoolean((new File(u.roots("FileSystem").cells("cwd").asInstanceOf[AtomishString].value, file_name)).exists)
    case _                                    => AtomishUnset //Should soft error
  }),
  "removeFile!"      -> AlienProxy(_.args match {
    case List(Left(AtomishString(file_name))) => AtomishBoolean((new File(u.roots("FileSystem").cells("cwd").asInstanceOf[AtomishString].value, file_name)).delete())
  }),
  "parentOf"         -> AlienProxy(_.args match {
    case List(Left(AtomishString(file_name))) => AtomishString((new File(u.roots("FileSystem").cells("cwd").asInstanceOf[AtomishString].value, file_name)).getParent())
  }),
  "withOpenFile"     -> AlienProxy(_.args match {
    case List(Left(AtomishString(file_name)), Left(lexical_thingy)) => {
      var io_file = new File(u.roots("FileSystem").cells("cwd").asInstanceOf[AtomishString].value, file_name)
      var writer = new FileWriter(io_file);
      var io = AtomishOrigin(MMap[String, AtomishThing](
        "put"   -> AlienProxy(_.args match {
          case List(Left(x)) => { writer.write(PreScalaPrinter.print(x)); AtomishUnset }
        }),
        "flush" -> AlienProxy(x => {writer.flush(); AtomishUnset})
      ))
      var ret = lexical_thingy match {
        case q: AlienProxy  => q.activate(AtomishArgs(List(Left(io))))
        case q: QAlienProxy => q.activate(AtomishCommated(Array(io)))
      }
      //io.cells("flush").asInstanceOf[AlienProxy].activate(AtomishArgs(List()))
      writer.flush()
      writer.close()
      ret
    }
  })
))
AtomishThing.post_bootstrap ++= MMap[(String, String), AtomishThing => AtomishThing](
  ("Boolean", "==")      -> { thing => AlienProxy(booltobool(_ == thing.asInstanceOf[AtomishBoolean].value)) },
  ("Boolean", "and")     -> { thing => AlienProxy(booltobool(_ && thing.asInstanceOf[AtomishBoolean].value)) },
  ("Boolean", "or")      -> { thing => AlienProxy(booltobool(_ || thing.asInstanceOf[AtomishBoolean].value)) },
  ("Boolean", "not")     -> { thing => AlienProxy(a => AtomishBoolean(!thing.asInstanceOf[AtomishBoolean].value)) },
  ("Boolean", "isTrue")  -> { thing => AlienProxy(a => AtomishBoolean(thing.asInstanceOf[AtomishBoolean].value)) },
  ("Boolean", "isFalse") -> { thing => AlienProxy(a => AtomishBoolean(!thing.asInstanceOf[AtomishBoolean].value)) },
  ("Boolean", "asText")  -> { thing => AlienProxy(a => AtomishString(thing.asInstanceOf[AtomishBoolean].value.toString())) },
  ("Array", "each")      -> { thing => QAlienProxy(_.args match {
    case Array(message) => {
      thing.asInstanceOf[AtomishArray].value.foreach(inner =>
          u.roots("eval").asInstanceOf[AlienProxy].activate(AtomishArgs(List(Left(message), Left(inner))))
      )
      AtomishUnset
    }
    case Array(AtomishMessage(variable), code) => {
      thing.asInstanceOf[AtomishArray].value.foreach(inner => {
          u.scopes = MMap(variable -> inner) +: u.scopes;
          u.roots("eval").asInstanceOf[AlienProxy].activate(AtomishArgs(List(Left(code))))
          var sco = u.scopes.tail;
          u.scopes = sco
      })
      AtomishUnset
    }
  }) },
  ("Array", "map")       -> { thing => QAlienProxy(_.args match {
    case Array(message) => {
      AtomishArray(thing.asInstanceOf[AtomishArray].value.map(inner =>
          u.roots("eval").asInstanceOf[AlienProxy].activate(AtomishArgs(List(Left(message), Left(inner))))
      ))
    }
    case Array(AtomishMessage(variable), code) => {
      AtomishArray(thing.asInstanceOf[AtomishArray].value.map(inner => {
          u.scopes = MMap(variable -> inner) +: u.scopes;
          var ret = u.roots("eval").asInstanceOf[AlienProxy].activate(AtomishArgs(List(Left(code))))
          var sco = u.scopes.tail;
          u.scopes = sco
          ret
      }))
    }
  }) },
  ("Array", "flatMap")       -> { thing => QAlienProxy(_.args match {
    case Array(message) => {
      AtomishArray(thing.asInstanceOf[AtomishArray].value.flatMap(inner =>
          u.roots("eval").asInstanceOf[AlienProxy].activate(AtomishArgs(List(Left(message),
            Left(inner)))).asInstanceOf[AtomishArray].value
      ))
    }
    case Array(AtomishMessage(variable), code) => {
      AtomishArray(thing.asInstanceOf[AtomishArray].value.flatMap(inner => {
          u.scopes = MMap(variable -> inner) +: u.scopes;
          var ret = u.roots("eval").asInstanceOf[AlienProxy].activate(AtomishArgs(List(Left(code)))).asInstanceOf[AtomishArray].value
          var sco = u.scopes.tail;
          u.scopes = sco
          ret
      }))
    }
  }) },
  ("Origin", "=")        -> { thing => QAlienProxy(_.args match {
    case Array(AtomishMessage(cell_name), x) => {
      var ret = u.roots("eval").asInstanceOf[AlienProxy].activate(AtomishArgs(List(Left(x))))
      thing.cells(cell_name) = ret
      ret
    }
  }) }
)

var prelude_source = new BufferedSource(new FileInputStream(new File("./prelude.atomish")))
var prelude = AtomishString(prelude_source.addString(new StringBuilder(1024)).toString())
e(r.read(prelude), None)

