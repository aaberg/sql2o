---
title: Changelog
layout: default
base_url: ../
---
# change log

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
