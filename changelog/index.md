---
title: Changelog
layout: default
base_url: ../
---
# change log

## sql2o 1.5.1 released
_<small>27 june 2014 <a href="https://github.com/aaberg">Lars Aaberg</a> </small>_

####- Fixes a bug with column mappings <a href="https://github.com/aaberg/sql2o/issues/132"><span class="badge badge-info">#132</span></a>
>Sql2o would silently ignore, if a column could not be mapped to a property. Sql2o will now throw an exception instead.
>_<small>Contributed by [Lars Aaberg](https://github.com/aaberg)</small>_

<br/>
<br/>

## sql2o 1.5.0 released
_<small>26 june 2014 <a href="https://github.com/aaberg">Lars Aaberg</a> </small>_

>No changes since version 1.5.0-RC2

<br/>
<br/>

## sql2o 1.5.0-RC2 released
_<small>23 june 2014 <a href="https://github.com/aaberg">Lars Aaberg</a> </small>_


####- Fixes a bug in OracleQuirks <a href="https://github.com/aaberg/sql2o/issues/130"><span class="badge badge-info">#130</span></a>
>The getGeneratedKeysByDefault flag was mistakenly set to `true` for OracleQuirks. Changed back to `false`.
>_<small>Contributed by [Lars Aaberg](https://github.com/aaberg)</small>_

####- Adds `setAutoCloseConnection(boolean)` on `ResultSetIterable` interface <a href="https://github.com/aaberg/sql2o/issues/130"><span class="badge badge-info">#131</span></a>
>_<small>Contributed by [Lars Aaberg](https://github.com/aaberg)</small>_

<br/>
<br/>

## sql2o 1.5.0-RC1 released
_<small>29 may 2014 <a href="https://github.com/aaberg">Lars Aaberg</a> </small>_

I am proud to announce the release of sql2o 1.5.0-RC1! In this release, a lot of refactoring has been done that improves performance and flexibility of the library. This is a release candidate, so if no major errors are found, this will become version 1.5.0

**A special thanks to [Dmitry Alexandrov](https://github.com/dimzon) for all his good ideas, and all the work he has put into this release.**	
    
<br/>
[List of changes on github](https://github.com/aaberg/sql2o/issues?labels=&milestone=8&page=1&state=closed)

####- Deprecated some of the old way of doing things <a href="https://github.com/aaberg/sql2o/pull/121"><span class="badge badge-info">#121</span></a>
>The execute methods are deprecated on Sql2o instance. It is now recommended to open a connection in a try-with-resource block, and call execute methods on that.
>_<small>Contributed by [Lars Aaberg](https://github.com/aaberg)</small>_

####- Introducing [mockito test framework](https://code.google.com/p/mockito/) for better unit testing <a href="https://github.com/aaberg/sql2o/issues/117"><span class="badge badge-info">#117</span></a>
>_<small>Contributed by [Dmitry Alexandrov](https://github.com/dimzon)</small>_

####- New method `withParams` for shorten syntax <a href="https://github.com/aaberg/sql2o/pull/115"><span class="badge badge-info">#115</span></a>
>_<small>Contributed by [Dmitry Alexandrov](https://github.com/dimzon)</small>_

####- Changed target language level to java 1.7 <a href="https://github.com/aaberg/sql2o/issues/114"><span class="badge badge-info">#114</span></a>
>_<small>Contributed by [Lars Aaberg](https://github.com/aaberg)</small>_

####- Better jodatime handling <a href="https://github.com/aaberg/sql2o/pull/112"><span class="badge badge-info">#112</span></a>
>This pull request also improves logging, StringConverter, FeatureDetector.
>_<small>Contributed by [Dmitry Alexandrov](https://github.com/dimzon)</small>_	

####- Keep tack of all statements created on connection, and close them when connection is closed <a href="https://github.com/dimzon/sql2o/commit/ffecad1bda59895e573f9d32e52526439aec1384"><span class="badge badge-info">ffecad1</span></a>
>_<small>Contributed by [Dmitry Alexandrov](https://github.com/dimzon)</small>_

####- Query now implements AutoClosable <a href="https://github.com/aaberg/sql2o/issues/110"><span class="badge badge-info">#110</span></a><a href="https://github.com/aaberg/sql2o/issues/112"><span class="badge badge-info">#112</span></a>
>_<small>Contributed by [Dmitry Alexandrov](https://github.com/dimzon)</small>_

####- Tables and Rows now use quirks <a href="https://github.com/aaberg/sql2o/pull/105"><span class="badge badge-info">#105</span></a>
>_<small>Contributed by [Dmitry Alexandrov](https://github.com/dimzon)</small>_

####- Added local converters <a href="https://github.com/aaberg/sql2o/pull/104"><span class="badge badge-info">#104</span></a>
>Extremely useful when using multiple instances of sql2o.
>_<small>Contributed by [Dmitry Alexandrov](https://github.com/dimzon)</small>_

####- Fixed incompatibility bug in some converters <a href="https://github.com/aaberg/sql2o/issues/102"><span class="badge badge-info">#102</span></a>
>_<small>Contributed by [Dmitry Alexandrov](https://github.com/dimzon)</small>_	

####- Fixed bug in EnumConverter <a href="https://github.com/aaberg/sql2o/issues/101"><span class="badge badge-info">#101</span></a>
>_<small>Contributed by [Dmitry Alexandrov](https://github.com/dimzon)</small>_	

####- Fixed thread safety issue on Convert class <a href="https://github.com/aaberg/sql2o/issues/100"><span class="badge badge-info">#100</span></a>
>_<small>Contributed by [Dmitry Alexandrov](https://github.com/dimzon)</small>_	

####- Added support for custom resultset mapper <a href="https://github.com/aaberg/sql2o/pull/97"><span class="badge badge-info">#97</span></a>
>_<small>Contributed by [Dmitry Alexandrov](https://github.com/dimzon)</small>_

####- Refactored handling of named parameters. <a href="https://github.com/aaberg/sql2o/pull/95"><span class="badge badge-info">#95</span></a>
>_<small>Contributed by [Lars Aaberg](https://github.com/aaberg)</small>_

####- Fixed UnderscoreToCamelCase thread-unsafe issue <a href="https://github.com/aaberg/sql2o/pull/93"><span class="badge badge-info">#93</span></a>
>_<small>Contributed by [Dmitry Alexandrov](https://github.com/dimzon)</small>_

####- NamedParameterStatement fails on Postgresql :: cast syntax <a href="https://github.com/aaberg/sql2o/issues/90"><span class="badge badge-info">90</span></a>
>_<small>Fixed by [Dmitry Alexandrov](https://github.com/dimzon)</small>_

####- Rewrite Query.Bind <a href="https://github.com/aaberg/sql2o/pull/87"><span class="badge badge-info">#87</span></a>
>_<small>Contributed by [Dmitry Alexandrov](https://github.com/dimzon)</small>_

####- More unit testing <a href="https://github.com/aaberg/sql2o/issues/86"><span class="badge badge-info">#86</span></a>
>_<small>Contributed by [Dmitry Alexandrov](https://github.com/dimzon)</small>_

####- Changed caching behavior of PojoMetadata <a href="https://github.com/aaberg/sql2o/issues/81"><span class="badge badge-info">81</span></a><a href="https://github.com/aaberg/sql2o/pull/85"><span class="badge badge-info">85</span></a>
>From now on, PojoMetadata instances will always be cached for improved performance
>_<small>Pointed out by [Dmitry Alexandrov](https://github.com/dimzon), Fixed by [Lars Aaberg](https://github.com/aaberg)</small>_

####- Refactoring for more flexibility and better performance <a href="https://github.com/aaberg/sql2o/pull/75"><span class="badge badge-info">#75</span></a>
>Doing things in a better way!
>_<small>Contributed by [Dmitry Alexandrov](https://github.com/dimzon)</small>_

####- Fixed a thread safety issue on PojoMetadata class <a href="https://github.com/aaberg/sql2o/pull/68"><span class="badge badge-info">#68</span></a><a href="https://github.com/aaberg/sql2o/pull/69"><span class="badge badge-info">#69</span></a>
>_<small>Pointed out by [Dmitry Alexandrov](https://github.com/dimzon), fixed by [Dmitry Alexandrov](https://github.com/dimzon) and [Lars Aaberg](https://github.com/aaberg)</small>_

####- Greatly improved performance of object mapper using `sun.misc.Unsafe` <a href="https://github.com/aaberg/sql2o/pull/67"><span class="badge badge-info">#67</span></a>
>Will fallback to using reflection if `sun.misc.Unsafe` is not availlable
>_<small>Contributed by [Dmitry Alexandrov](https://github.com/dimzon)</small>_

####- Convert table to list of maps <a href="https://github.com/aaberg/sql2o/pull/63"><span class="badge badge-info">#63</span></a><a href="https://github.com/aaberg/sql2o/pull/66"><span class="badge badge-info">#66</span></a>
>It is now possible to convert a Table instance to a list of maps. This is done with the new `Table.asList()` method. There is also added a similar `Row.asMap()` method for converting a row to a map.
>_<small>Proposed by [Sachin Walia](https://github.com/sachinwalia2k8). Implemented by [Alden Quimby](https://github.com/aldenquimby)</small>_

####- Bidirectional converters <a href="https://github.com/aaberg/sql2o/issues/62"><span class="badge badge-info">#62</span></a><a href="https://github.com/aaberg/sql2o/issues/70"><span class="badge badge-info">#70</span></a>
>Bidirectional converters allows for converters to specify conversion both when data is parsed from a ResultSet and when setting parameters.
>_<small>Contributed by [Alden Quimby](https://github.com/aldenquimby)</small>_

####- Better quirks pattern <a href="https://github.com/aaberg/sql2o/issues/62"><span class="badge badge-info">#62</span></a><a href="https://github.com/aaberg/sql2o/issues/70"><span class="badge badge-info">#70</span></a>
>The better Quirks pattern allows for much better control.
>_<small>Proposed by [Dmitry Alexandrov](https://github.com/dimzon). Implemented by [Alden Quimby](https://github.com/aldenquimby) & [Dmitry Alexandrov](https://github.com/dimzon)</small>_

####- New project structure for better extensions support <a href="https://github.com/aaberg/sql2o/pull/61"><span class="badge badge-info">#61</span></a>
>_<small>Contributed by [Lars Aaberg](https://github.com/aaberg)</small>_

####- Bug in inserting date time in mysql, postgres <a href="https://github.com/aaberg/sql2o/issues/59"><span class="badge badge-info">#59</span></a><a href="https://github.com/aaberg/sql2o/issues/60"><span class="badge badge-info">#60</span></a>
>When setting a datetime parameter, only the date would be updated/inserted in the database.
>_<small>Fixed by [Alden Quimby](https://github.com/aldenquimby)</small>_

####- A better Query.bind method <a href="https://github.com/aaberg/sql2o/issues/55"><span class="badge badge-info">#55</span></a><a href="https://github.com/aaberg/sql2o/issues/80"><span class="badge badge-info">#80</span></a>
>Fixes a bug and improves behaviour.
>_<small>Contributed by [Dmitry Alexandrov](https://github.com/dimzon)</small>_


<br/>
<br/>

## Sql2o 1.4.2 released
_<small>29 apr 2014 <a href="https://github.com/aaberg">Lars Aaberg</a> </small>_

####- Fixed thread safety issue on enum converterd <a href="https://github.com/aaberg/sql2o/pull/116"><span class="badge badge-info">#116</span></a>
>As the heading say - Fixed a thread safety issue.
>_<small>Fixed by [Lars Aaberg](https://github.com/aaberg)</small>_

####- Added Oracle QuirksMode <a href="https://github.com/aaberg/sql2o/commit/47eba9361fa1a5a38aff048bba28cf682dea827c"><span class="badge badge-info">47eba93</span></a>
>Using this QuirksMode disables returning of generated keys by default, as Oracle JDBC driver otherwise throws an OperationNotAllowed exception for most queries.
>_<small>Fixed by [Lars Aaberg](https://github.com/aaberg)</small>_

<br/>
<br/>

## Sql2o 1.4.1 released
_<small>22 apr 2014 <a href="https://github.com/aaberg">Lars Aaberg</a> </small>_

####- Fixed statement-leak <a href="https://github.com/aaberg/sql2o/pull/113"><span class="badge badge-info">#113</span></a>
>When using autoClose feature, statements was never explicitly closed.
>_<small>Discovered by [Alden Quimby](https://github.com/aldenquimby)</small>_

<br/>
<br/>

## Sql2o 1.4.0 released
_<small>31 mar 2014 <a href="https://github.com/aaberg">Lars Aaberg</a> </small>_

####- Performance testing against other frameworks <a href="https://github.com/aaberg/sql2o/issues/54"><span class="badge badge-info">#54</span></a>
>Guess who won! Check out the result [here](https://github.com/aaberg/sql2o)
>_<small>Mainly contributed by [Alden Quimby](https://github.com/aldenquimby)</small>_


####- Added executeAndFetchTableLazy method <a href="https://github.com/aaberg/sql2o/issues/49"><span class="badge badge-info">#49</span></a>
>_<small>Contributed by [Alden Quimby](https://github.com/aldenquimby)</small>_

####- Made Connection class AutoClosable <a href="https://github.com/aaberg/sql2o/issues/51"><span class="badge badge-info">#51</span></a>
>_<small>Contributed by [Lars Aaberg](https://github.com/aaberg)</small>_

####- Made it possible to run multiple statements on the same Connection instance <a href="https://github.com/aaberg/sql2o/issues/52"><span class="badge badge-info">#52</span></a>
>On damn time!
>_<small>Contributed by [Lars Aaberg](https://github.com/aaberg)</small>_

####- Added ability to return keys from a batch <a href="https://github.com/aaberg/sql2o/issues/57"><span class="badge badge-info">#57</span></a>
>_<small>Contributed by [Alden Quimby](https://github.com/aldenquimby)</small>_

####- ...And some generel bug fixing.
> [Full list of fixed issues in this version](https://github.com/aaberg/sql2o/issues?milestone=7&page=1&state=closed)


<br/>
<br/>


## Sql2o 1.3.0 released
_<small>10 feb 2014 <a href="https://github.com/aaberg">Lars Aaberg</a> </small>_

#####- Make joda time dependency optional <a href="https://github.com/aaberg/sql2o/pull/47"><span class="badge badge-info">#47</span></a>  
>For those of you who doesn't use jodatime.
>_<small>Contributed by [Alden Quimby](https://github.com/aldenquimby)</small>_

#####- executeAndFetch method can fallback to use executeScalar methods<a href="https://github.com/aaberg/sql2o/pull/46"><span class="badge badge-info">#46</span></a>  
>`executeAndFetch` and `executeAndFetchFirst` methods can fallback to use `executeScalar` and `executeScalarList` methods. This makes the api easier to use.
>_<small>Contributed by [Alden Quimby](https://github.com/aldenquimby)</small>_



#####- Moved to maven central repository for hosting <a href="https://github.com/aaberg/sql2o/issues/44"><span class="badge badge-info">#44</span></a>  
>On damn time!
>_<small>Contributed by [Lars Aaberg](https://github.com/aaberg)</small>_

#####- added executeAndFetchLazy(Class) method <a href="https://github.com/aaberg/sql2o/pull/42"><span class="badge badge-info">#42</span></a>  
>Using the `executeAndFetchLazy(Class)` method greatly improves performance on large datasets. Check out the documentation <a href="{{page.base_url}}docs/fetching-data-lazy">here</a>
>_<small>Contributed by [Alden Quimby](https://github.com/aldenquimby)</small>_

#####- Custom enum converter <a href="https://github.com/aaberg/sql2o/pull/39"><span class="badge badge-info">#39</span></a>  
>It is now possible to add a custom enum converter. For use when you want to change default behavior of the enum converter.
>_<small>Contributed by [Alden Quimby](https://github.com/aldenquimby)</small>_

#####- Make Slf4j dependeny optional <a href="https://github.com/aaberg/sql2o/issues/37"><span class="badge badge-info">#37</span></a>  
>If Slf4j is not in class path, sql2o will log directly to System.err.
>_<small>Contributed by [Lars Aaberg](https://github.com/aaberg)</small>_

A special thanks to [Alden Quimby](https://github.com/aldenquimby), who has done most of the work for this release.


<br/>
<br/>


## Sql2o 1.2.1 released
_<small>21 jan 2014 <a href="https://github.com/aaberg">Lars Aaberg</a> </small>_

#####- Bug fix   <a href="https://github.com/aaberg/sql2o/issues/41"><span class="badge badge-info">#41</span></a>
>when using the Query.addParameter(String, Date) method, IBM DB2 jdbc driver would throw an exception when column is 'date' datatype   
>_<small>Fixed by [Lars Aaberg](https://github.com/aaberg)</small>_


<br/>
<br/>

## Sql2o 1.2.0 released
_<small>14 jan 2014 <a href="https://github.com/aaberg">Lars Aaberg</a> </small>_

#####- Auto mapping of field names with underscore.  <a href="https://github.com/aaberg/sql2o/pull/33"><span class="badge badge-info">#33</span></a>  
>As of version 1.2.0, a database column with name “field_name”, will automatically be mapped to a java property with name “fieldName”.  
>_<small>Contributed by [Ryan Carlson](https://github.com/ryancarlson)</small>_



#####- getObject() method now has an overload that uses a registered converter. <a href="https://github.com/aaberg/sql2o/issues/32"><span class="badge badge-info">#32</span></a>
>Example   
>MyModel value = query.getObject(“colname”, MyModel.class);    
>_<small>contributed by [Lars Aaberg](https://github.com/aaberg)</small>_


#####- Changed setColumnMappings(Map) method public instead of private. And changed the method to return the Query instance, to support chaining.  <a href="https://github.com/aaberg/sql2o/pull/23"><span class="badge badge-info">#23</span></a>
>_<small>contributed by [kaliy](https://github.com/kaliy)</small>_


#####- New LocalTime converter.   <a href="https://github.com/aaberg/sql2o/issues/22"><span class="badge badge-info">#22</span></a>
>_<small>contributed by [Lars Aaberg](https://github.com/aaberg)</small>_


#####- Added JNDI support  <a href="https://github.com/aaberg/sql2o/pull/21"><span class="badge badge-info">#21</span></a>   
>_<small>Contributed by [Manuel de la Peña](https://github.com/mdelapenya)</small>_


#####- Added Query.bind(Object bean) method   <a href="https://github.com/aaberg/sql2o/pull/20"><span class="badge badge-info">#20</span></a>   
>Example:   
>String sql = “insert into mytable(co1, col2, col3) values (:prop1, :prop2, prop3)”;   
>If we have a POJO with properties prop1, prop2 and prop3, we can call:   
>sql2o.createQuery(sql).bind(myPojo).executeUpdate();   
>_<small>Contributed by [Juan Noriega](https://github.com/jsnoriegam)</small>_


#####- Added new UUID converter   <a href="https://github.com/aaberg/sql2o/pull/17"><span class="badge badge-info">#17</span></a>
>_<small>Contributed by [Tomasz Kubacki](https://github.com/tomaszkubacki)</small>_


#####- Support for java.io.InputStream when inserting into blobs   <a href="https://github.com/aaberg/sql2o/pull/15"><span class="badge badge-info">#15</span></a>
>_<small>Contributed by [rwozniak](https://github.com/rwozniak)</small>_


... And a lot of bugs has been fixed!

Thanks for all the contributions. You guys are amazing!

I will try to get the documentation updated with the new features soon. Help is greatly appreciated.

[@l_aaberg](https://twitter.com/l_aaberg)
