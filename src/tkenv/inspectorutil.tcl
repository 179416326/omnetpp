#=================================================================
#  INSPECTORUTIL.TCL - part of
#
#                     OMNeT++/OMNEST
#            Discrete System Simulation in C++
#
#=================================================================

#----------------------------------------------------------------#
#  Copyright (C) 1992-2008 Andras Varga
#
#  This file is distributed WITHOUT ANY WARRANTY. See the file
#  `license' for details on this and other legal matters.
#----------------------------------------------------------------#


proc textWindowAddIcons {w {wintype ""}} {
    global icons help_tips

    packIconButton $w.toolbar.copy   -image $icons(copy) -command "editCopy $w.main.text"
    packIconButton $w.toolbar.find   -image $icons(find) -command "findDialog $w.main.text"
    packIconButton $w.toolbar.save   -image $icons(save) -command "saveFile $w"
    if {$wintype=="modulewindow"} {
        packIconButton $w.toolbar.filter -image $icons(filter) -command "editFilterWindowContents $w"
    }
    packIconButton $w.toolbar.sep21  -separator

    set help_tips($w.toolbar.copy)   {Copy selected text to clipboard (Ctrl+C)}
    set help_tips($w.toolbar.find)   {Find string in window (Ctrl+F)}
    set help_tips($w.toolbar.save)   {Save window contents to file}
    set help_tips($w.toolbar.filter) {Filter window contents (Ctrl+H)}
}

proc createInspectorListbox {f w} {
    global B2 B3

    label $f.label -text "# objects:"
    pack $f.label -side top -anchor w

    frame $f.main
    pack $f.main -expand 1 -fill both -side top

    set lb $f.main.list
    ttk::treeview $lb -columns {name info ptr} -show {tree headings} -yscroll "$f.main.vsb set" -xscroll "$f.main.hsb set"
    $lb heading "#0" -anchor c -text "Class"   ;#TODO: -command [list inspectorListbox:sortBy $lb "#0" 0]
    $lb heading name -anchor w -text "Name"    -command [list inspectorListbox:sortBy $lb name 0]
    $lb heading info -anchor w -text "Info"    -command [list inspectorListbox:sortBy $lb info 0]
    $lb heading ptr  -anchor w -text "Pointer" -command [list inspectorListbox:sortBy $lb ptr 0]
    $lb column "#0"  -stretch 0 -width 140
    $lb column name  -stretch 0 -width 120
    $lb column info  -stretch 0 -width 300

    scrollbar $f.main.hsb  -command "$f.main.list xview" -orient horiz
    scrollbar $f.main.vsb  -command "$f.main.list yview"
    grid $f.main.list $f.main.vsb -sticky news
    grid $f.main.hsb  x           -sticky news
    grid rowconfig    $f.main 0 -weight 1 -minsize 0
    grid columnconfig $f.main 0 -weight 1 -minsize 0
    #FIXME TODO: -width 400

    bind $f.main.list <<TreeviewSelect>> [list inspectorListbox:selectionChanged $w %W]
    bind $f.main.list <Double-Button-1> {inspectorListbox:dblClick %W}
    bind $f.main.list <Button-$B3> [list +inspectorListbox:rightClick $w %W %X %Y]  ;# Note "+"! it appends this code to binding in widgets.tcl
    bind $f.main.list <Key-Return> [list inspectorListbox:dblClick %W]

    focus $f.main.list

    return $f.main.list
}

# source: Tk "widget" demo
proc inspectorListbox:sortBy {tree col direction} {
    #TODO: cannot sort by the #0 column
    # Determine currently sorted column and its sort direction
    foreach c {"#0" name info ptr} {
        set s [$tree heading $c state]
        if {("selected" in $s || "alternate" in $s) && $col ne $c} {
            # Sorted column has changed
            $tree heading $c state {!selected !alternate !user1}
            set direction [expr {"alternate" in $s}]
        }
    }

    # Build something we can sort
    set data {}
    foreach row [$tree children {}] {
        lappend data [list [$tree set $row $col] $row]
    }

    set dir [expr {$direction ? "-decreasing" : "-increasing"}]
    set r -1

    # Now reshuffle the rows into the sorted order
    foreach info [lsort -dictionary -index 0 $dir $data] {
        $tree move [lindex $info 1] {} [incr r]
    }

    # Switch the heading so that it will sort in the opposite direction
    $tree heading $col -command [list inspectorListbox:sortBy $tree $col [expr {!$direction}]] \
        state [expr {$direction?"!selected alternate":"selected !alternate"}]
    if {[tk windowingsystem] eq "aqua"} {
        # Aqua theme displays native sort arrows when user1 state is set
        $tree heading $col state "user1"
    }
}

proc inspectorListbox:getSelection {lb} {
    set ptrs {}
    foreach item [$lb selection] {
        set values [$lb item $item -values]
        set ptr [lindex $values 2]
        lappend ptrs $ptr
    }
    return $ptrs
}

proc inspectorListbox:getCurrent {lb} {
    set ptrs [inspectorListbox:getSelection $lb]
    return [lindex $ptrs 0]
}

proc inspectorListbox:selectionChanged {w lb} {
    set ptr [inspectorListbox:getCurrent $lb]
    if [opp_isnotnull $ptr] {
        mainWindow:selectionChanged $w $ptr
    }
}

proc inspectorListbox:rightClick {w lb X Y} {
    set ptr [inspectorListbox:getCurrent $lb]
    if [opp_isnotnull $ptr] {
        set popup [createInspectorContextMenu $w $ptr]
        tk_popup $popup $X $Y
    }
}

proc inspectorListbox:dblClick {lb} {
    set ptr [inspectorListbox:getCurrent $lb]
    if [opp_isnotnull $ptr] {
        opp_inspect $ptr {(default)}
    }
}

#
# Extends context menu with extra items. Example config for the INET Framework:
#
# extendContextMenu {
#    {"INET: Interfaces"             "**"  "**interfaceTable.interfaces"     "*vector*"}
#    {"INET: IP Routing Table"       "**"  "**routingTable.routes"           "*vector*"}
#    {"INET: IP Multicast Routes"    "**"  "**routingTable.multicastRoutes"  "*vector*"}
#    {"INET: IPv6 Routing Table"     "**"  "**routingTable6.routeList"       "*vector*"}
#    {"INET: IPv6 Destination Cache" "**"  "**routingTable6.destCache"       "*map*"   }
#    {"INET: ARP cache"              "**"  "**arp.arpCache"                  "*map*"   }
#    {"INET: TCP connections"        "**"  "**tcp.tcpAppConnMap"             "*map*"   }
#
#    {"INET: Interfaces"             "**.interfaceTable"  "interfaces"      "*vector*"}
#    {"INET: IP Routing Table"       "**.routingTable"    "routes"          "*vector*"}
#    {"INET: IP Multicast Routes"    "**.routingTable"    "multicastRoutes" "*vector*"}
#    {"INET: IPv6 Routing Table"     "**.routingTable6"   "routeList"       "*vector*"}
#    {"INET: IPv6 Destination Cache" "**.routingTable6"   "destCache"       "*map*"   }
#    {"INET: ARP cache"              "**.arp"             "arpCache"        "*map*"   }
#    {"INET: TCP connections"        "**.tcp"             "tcpAppConnMap"   "*map*"   }
# }
#
proc extendContextMenu {rules} {
    global contextmenurules

    set i [llength $contextmenurules(keys)]
    foreach line $rules {
       lappend contextmenurules(keys) $i
       if {[llength $line]!=4} {
           set rulename "\"[lindex $line 0]\""
           tk_messageBox -type ok -icon info -title Info -message "Context menu inspector rule $rulename should contain 4 items, ignoring."
       } else {
           set contextmenurules($i,label)   [lindex $line 0]
           set contextmenurules($i,context) [lindex $line 1]
           set contextmenurules($i,name)    [lindex $line 2]
           set contextmenurules($i,class)   [lindex $line 3]
       }
       incr i
    }
}

proc fillInspectorContextMenu {menu w ptr} {
    global contextmenurules

    # ptr should never be null, but check it anyway
    if [opp_isnull $ptr] {return $menu}

    # add inspector types supported by the object
    set name [opp_getobjectfullname $ptr]
    set insptypes [opp_supported_insp_types $ptr]
    if {$w!="" && $ptr!=[opp_inspector_getobject $w]} {
        if [opp_inspector_supportsobject $w $ptr] {set state normal} else {set state disabled}
        $menu add command -label "Go into '$name'" -command "opp_inspector_setobject $w $ptr" -state $state
        $menu add separator
    }
    foreach type $insptypes {
       $menu add command -label "Inspect $type..." -command "opp_inspect $ptr \{$type\}"
    }

    # add "run until" menu items
    set baseclass [opp_getobjectbaseclass $ptr]
    if {$baseclass=="cSimpleModule" || $baseclass=="cCompoundModule"} {
        set w ".$ptr-0"  ;#hack
        $menu add separator
        $menu add command -label "Run until next event in module '$name'" -command "runSimulationLocal $w normal"
        $menu add command -label "Fast run until next event in module '$name'" -command "runSimulationLocal $w fast"
    }

    if {$baseclass=="cMessage"} {
        $menu add separator
        $menu add command -label "Run until message '$name'" -command "runUntilMsg $ptr normal"
        $menu add command -label "Fast run until message '$name'" -command "runUntilMsg $ptr fast"
        $menu add command -label "Express run until message '$name'" -command "runUntilMsg $ptr express"
        $menu add separator
        $menu add command -label "Exclude messages like '$name' from animation" -command "excludeMessageFromAnimation $ptr"
    }

    # add further menu items
    set name [opp_getobjectfullpath $ptr]
    set allcategories "mqsgvo"
    set first 1
    foreach key $contextmenurules(keys) {
       #debug "trying $contextmenurules($key,label): opp_getsubobjectsfilt $ptr $allcategories $contextmenurules($key,class) $name.$contextmenurules($key,name) 1"
       # check context matches
       if ![opp_patmatch $name $contextmenurules($key,context)] {
           continue
       }
       # check we have such object
       # Note: we have to quote the pattern for the object matcher, because $name might contain spaces
       set objlist [opp_getsubobjectsfilt $ptr $allcategories $contextmenurules($key,class) "\"$name.$contextmenurules($key,name)\"" 1 ""]
       if {$objlist!={}} {
           if {$first} {
               set first 0
               $menu add separator
           }
           $menu add command -label "$contextmenurules($key,label)..." -command "inspectContextMenuRules $ptr $key"
       }
    }
}

proc createInspectorContextMenu {w ptrs} {

    # create popup menu
    catch {destroy .popup}
    menu .popup -tearoff 0

    if [opp_isinspector $w] {
       set ptr [opp_inspector_getobject $w]
       if [opp_isnotnull $ptr] {
          set parentptr [opp_getobjectowner $ptr]
          if {[opp_isnotnull $parentptr] && [opp_inspector_supportsobject $w $parentptr]} {
              .popup add command -label "Go up" -command "opp_inspector_setobject $w $parentptr"
              .popup add separator
          }
       }
    }

    if {[llength $ptrs] == 1} {
        fillInspectorContextMenu .popup $w $ptrs
    } else {
        foreach ptr $ptrs {
            set submenu .popup.$ptr
            catch {destroy $submenu}
            menu $submenu -tearoff 0
            set name [opp_getobjectfullname $ptr]
            set shorttypename [opp_getobjectshorttypename $ptr]
            set infostr "$shorttypename, [opp_getobjectinfostring $ptr]"
            if {[string length $infostr] > 30} {
                set infostr [string range $infostr 0 29]...
            }
            set baseclass [opp_getobjectbaseclass $ptr]
            if {$baseclass == "cGate" } {
                set nextgateptr [opp_getobjectfield $ptr "nextGate"]
                set nextgatename [opp_getobjectfullname $nextgateptr]
                set ownerptr [opp_getobjectowner $ptr]
                set ownername [opp_getobjectfullname $ownerptr]
                set nextgateownerptr [opp_getobjectowner $nextgateptr]
                set nextgateownername [opp_getobjectfullname $nextgateownerptr]

                set label "$ownername.$name --> $nextgateownername.$nextgatename"
            } elseif {$baseclass == "cMessage" } {
                set shortinfo [opp_getmessageshortinfostring $ptr]
                set label "$name ($shorttypename, $shortinfo)"
            } else {
                set label "$name ($infostr)"
            }
            fillInspectorContextMenu $submenu $w $ptr
            .popup add cascade -label $label -menu $submenu
        }
    }

    return .popup
}

proc inspectContextMenuRules {ptr key} {
    global contextmenurules
    set allcategories "mqsgvo"
    set name [opp_getobjectfullpath $ptr]
    set objlist [opp_getsubobjectsfilt $ptr $allcategories $contextmenurules($key,class) "$name.$contextmenurules($key,name)" 100 ""]
    if {[llength $objlist] > 5} {
        tk_messageBox -type ok -icon info -title Info -message "This matches [llength $objlist]+ objects, opening inspectors only for the first five."
        set objlist [lrange $objlist 0 4]
    }
    foreach objptr $objlist {
        opp_inspect $objptr "(default)"
    }
}

proc inspectThis {w type} {
    # extract object pointer from window path name and create inspector
    set object [opp_inspector_getobject $w]
    opp_inspect $object $type
}

proc inspectComponentType {w {type "(default)"}} {
    # extract object pointer from window path name and create inspector
    set ptr [opp_inspector_getobject $w]
    set typeptr [opp_getcomponenttypeobject $ptr]
    opp_inspect $typeptr $type
}

#
# Called from balloon.tcl, supposed to return tooltip for a widget (or items
# in a widget). Installed via: set help_tips(helptip_proc) getHelpTip
#
# Here we produce help text for canvas items that represent simulation
# objects.
#
proc getHelpTip {w x y} {
   if {![winfo exists $w]} {
       return ""
   }
   set tip ""

   if {[winfo class $w]=="Canvas"} {
     set canvasx [$w canvasx $x]
     set canvasy [$w canvasy $y]
     set items [$w find overlapping [expr $canvasx-2] [expr $canvasy-2] [expr $canvasx+2] [expr $canvasy+2]]

     set tip ""
     foreach item $items {
       # if this is a simulation object, get its pointer
       set ptr ""
       set tags [$w gettags $item]
       if {[lsearch $tags "tooltip"] == -1} {
          continue
       }

       if {[lsearch $tags "ptr*"] != -1} {
          regexp "ptr.*" $tags ptr
       } elseif {[lsearch $tags "qlen-ptr*"] != -1} {
          regexp "ptr.*" $tags modptr
          set ptr [ModuleInspector:qlenGetQptr [winfo parent $w] $modptr] ;# Khmm...
       } elseif {[lsearch $tags "node-ptr*"] != -1} {
          regexp "ptr.*" $tags ptr
       } elseif {[lsearch $tags "node-*"] != -1} {
          set i [lsearch $tags "node-*"]
          set tag [lindex $tags $i]
          regexp "node-(.*)" $tag match node
          return [Tree:gettooltip $w $node]
       }
       set ptr [lindex $ptr 0]

       if [opp_isnotnull $ptr] {
          append tip "([opp_getobjectshorttypename $ptr]) [opp_getobjectfullname $ptr]"
          set info [opp_getobjectinfostring $ptr]
          if {$info!=""} {append tip ", $info"}
          regsub {  +} $tip {  } tip
          if {[lsearch $tags "modname"] == -1} {
             set dispstr ""
             catch { set dispstr [opp_getobjectfield $ptr displayString] }
             set tt_tag [opp_displaystring $dispstr getTagArg "tt" 0 $ptr 1]
          } else {
             # if it has tag "modname", it is the enclosing module
             set dispstr ""
             catch { set dispstr [opp_getobjectfield $ptr displayString] }
             set tt_tag [opp_displaystring $dispstr getTagArg "bgtt" 0 $ptr 0]
          }
          if {$tt_tag!=""} {
             append tip "\n  $tt_tag"
          }
       }
       append tip "\n"
     }
   }
   return [string trim $tip \n]
}

