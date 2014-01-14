---
layout: default
title: Version 1.2.0 has been released!
base_url: ../../../
---

# Sql2o 1.2.0 released
_<small>14 jan 2014 <a href="https://github.com/aaberg">Lars Aaberg</a> </small>_

It has been quite a while since the last release of sql2o. But the project hasn't been dormant! A lot of people have
contributed and a lot of new features has been added and quite a few bugs has been fixed. I am excited to announce that
all the effort has resulted in the release of version 1.2.0 of sql2o!

Here is a list of new features:

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