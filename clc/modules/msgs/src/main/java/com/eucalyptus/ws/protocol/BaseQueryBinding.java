/*******************************************************************************
 * Copyright (c) 2009  Eucalyptus Systems, Inc.
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, only version 3 of the License.
 * 
 * 
 *  This file is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  for more details.
 * 
 *  You should have received a copy of the GNU General Public License along
 *  with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 *  Please contact Eucalyptus Systems, Inc., 130 Castilian
 *  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
 *  if you need additional information or have any questions.
 * 
 *  This file may incorporate work covered under the following copyright and
 *  permission notice:
 * 
 *    Software License Agreement (BSD License)
 * 
 *    Copyright (c) 2008, Regents of the University of California
 *    All rights reserved.
 * 
 *    Redistribution and use of this software in source and binary forms, with
 *    or without modification, are permitted provided that the following
 *    conditions are met:
 * 
 *      Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 * 
 *      Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 * 
 *    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 *    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 *    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 *    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 *    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 *    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
 *    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
 *    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
 *    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 *    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 *    THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */

package com.eucalyptus.ws.protocol;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import com.eucalyptus.binding.Binding;
import com.eucalyptus.binding.BindingException;
import com.eucalyptus.binding.BindingManager;
import com.eucalyptus.binding.HttpEmbedded;
import com.eucalyptus.binding.HttpParameterMapping;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.ws.handlers.RestfulMarshallingHandler;
import com.google.common.collect.Lists;
import edu.emory.mathcs.backport.java.util.Arrays;
import edu.ucsb.eucalyptus.msgs.BaseData;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;
import edu.ucsb.eucalyptus.msgs.EucalyptusMessage;
import groovy.lang.GroovyObject;

public class BaseQueryBinding<T extends Enum<T>> extends RestfulMarshallingHandler {
  private static Logger LOG = Logger.getLogger( BaseQueryBinding.class );
  private final T operationParam;
  private final List<T> altOperationParams;
  private final List<T> possibleParams;
  /**
   * @param namespacePattern - the format string to be used when constructing the namespace.  this can be a fully formed namespace.
   * @param operationParam - this argument is used to determine the list of possible operation parameters
   * @param alternativeOperationParam - these arguments are treated as alternatives to <tt>operationParam</tt> (e.g., <tt>Action</tt> is an alternative to <tt>Operation</tt>).
   */
  public BaseQueryBinding( String namespacePattern, T operationParam, T... alternativeOperationParam ) {
    super( namespacePattern );
    this.operationParam = operationParam;
    this.altOperationParams = Arrays.asList( alternativeOperationParam );
    this.possibleParams = Arrays.asList( operationParam.getDeclaringClass( ).getEnumConstants( ) );
  }

  /**
   * @param namespacePattern - the format string to be used when constructing the namespace.  this can be a fully formed namespace.
   * @param defaultVersion - default version to use if binding problems are encountered (e.g., unknown request namespace).
   * @param operationParam - this argument is used to determine the list of possible operation parameters
   * @param alternativeOperationParam - these arguments are treated as alternatives to <tt>operationParam</tt> (e.g., <tt>Action</tt> is an alternative to <tt>Operation</tt>).
   */
  public BaseQueryBinding( String namespacePattern, String defaultVersion, T operationParam, T... alternativeOperationParam ) {
    super( namespacePattern, defaultVersion );
    this.operationParam = operationParam;
    this.altOperationParams = Arrays.asList( alternativeOperationParam );
    this.possibleParams = Arrays.asList( operationParam.getDeclaringClass( ).getEnumConstants( ) );
  }

  private final String extractOperationName( final MappingHttpRequest httpRequest ) {
    if( httpRequest.getParameters( ).containsKey( this.operationParam.toString( ) ) ) {
      return httpRequest.getParameters( ).get( this.operationParam.toString( ) );
    } else {
      for( T param : this.altOperationParams ) {
        if( httpRequest.getParameters( ).containsKey( param.toString( ) ) ) {
          return httpRequest.getParameters( ).get( this.operationParam.toString( ) );
        }
      }
    }
    LOG.error( "Failed to find operation parameter an " + Lists.asList( this.operationParam, this.altOperationParams.toArray( ) ).toString( ) + " in HTTP request: " + httpRequest );
    return null;
  }
  
  @Override
  public Object bind( final MappingHttpRequest httpRequest ) throws BindingException {
    final String operationName = this.extractOperationName( httpRequest );
    final String operationNameType = operationName + "Type";
    for ( T op : this.possibleParams )
      httpRequest.getParameters( ).remove( op.name( ) );
    final Map<String, String> params = httpRequest.getParameters( );
    
    BaseMessage eucaMsg = null;
    Map<String, String> fieldMap = null;
    Class<?> targetType = null;
    Binding currentBinding = null;
    try {
      if( this.getBinding( ).hasElementClass( operationName ) ) {
        currentBinding = this.getBinding( );
        targetType = currentBinding.getElementClass( operationName );
      } else if( this.getBinding( ).hasElementClass( operationNameType ) ) {
        currentBinding = this.getBinding( );
        targetType = currentBinding.getElementClass( operationNameType );
      } else if( this.getDefaultBinding( ).hasElementClass( operationName ) ) {
        currentBinding = this.getDefaultBinding( );
        targetType = currentBinding.getElementClass( operationName );
      } else if( this.getDefaultBinding( ).hasElementClass( operationNameType ) ) {
        currentBinding = this.getDefaultBinding( );
        targetType = currentBinding.getElementClass( operationNameType );
      } else if( BindingManager.getDefaultBinding( ).hasElementClass( operationName ) ) {
        currentBinding = BindingManager.getDefaultBinding( );
        targetType = currentBinding.getElementClass( operationName );
      } else if( BindingManager.getDefaultBinding( ).hasElementClass( operationNameType ) ) {
        currentBinding = BindingManager.getDefaultBinding( );
        targetType = currentBinding.getElementClass( operationNameType );
      } else {//this will necessarily fault.
        try {
          targetType = this.getBinding( ).getElementClass( operationName );
        } catch ( BindingException ex ) {
          LOG.error( ex , ex );
          throw ex;
        }
      }
      fieldMap = this.buildFieldMap( targetType );
      eucaMsg = ( BaseMessage ) targetType.newInstance( );
    } catch ( BindingException e ) {
      LOG.debug( "Failed to construct message of type: " + operationName, e );
      LOG.error( e, e );
      throw e;
    } catch ( Throwable e ) {
      throw new BindingException( "Failed to construct message of type " + operationName, e );
    }
    
    List<String> failedMappings = populateObject( ( GroovyObject ) eucaMsg, fieldMap, params );
    
    if ( !failedMappings.isEmpty( ) || !params.isEmpty( ) ) {
      StringBuilder errMsg = new StringBuilder( "Failed to bind the following fields:\n" );
      for ( String f : failedMappings )
        errMsg.append( f ).append( '\n' );
      for ( Map.Entry<String, String> f : params.entrySet( ) )
        errMsg.append( f.getKey( ) ).append( " = " ).append( f.getValue( ) ).append( '\n' );
      throw new BindingException( errMsg.toString( ) );
    }
    
//TODO:GRZE:REVIEW    
    try {
      currentBinding.toOM( eucaMsg, this.getNamespace( ) );
    } catch ( RuntimeException e ) {
      LOG.error( "Falling back to default (unvalidated) binding for: " + operationName + " with params=" + params );
      LOG.error( "Failed to build a valid message: " + e.getMessage( ), e );
      try {
        BindingManager.getDefaultBinding( ).toOM( eucaMsg, BindingManager.DEFAULT_BINDING_NAMESPACE );
      } catch ( RuntimeException ex ) {
        throw new BindingException( "Default binding failed to build a valid message: " + ex.getMessage( ), ex );
      }
    }
    return eucaMsg;
  }
  
  private static Field getRecursiveField( Class<?> clazz, String fieldName ) throws Exception {
    Field ret = null;
    Exception e = null;
    while ( !BaseMessage.class.equals( clazz ) || !Object.class.equals( clazz ) ) {
      try {
        ret = clazz.getDeclaredField( fieldName );
        return ret;
      } catch ( Exception e1 ) {
        e = e1;
      }
      clazz = clazz.getSuperclass( );
    }
    if ( ret == null ) {
      throw e;
    }
    return ret;
  }
  
  private List<String> populateObject( final GroovyObject obj, final Map<String, String> paramFieldMap, final Map<String, String> params ) {
    List<String> failedMappings = new ArrayList<String>( );
    for ( Map.Entry<String, String> e : paramFieldMap.entrySet( ) ) {
      try {
        if ( getRecursiveField( obj.getClass( ), e.getValue( ) ).getType( ).equals( ArrayList.class ) ) failedMappings.addAll( populateObjectList( obj,
                                                                                                                                                   e,
                                                                                                                                                   params,
                                                                                                                                                   params.size( ) ) );
      } catch ( Exception e1 ) {
        failedMappings.add( e.getKey( ) );
      }
    }
    for ( Map.Entry<String, String> e : paramFieldMap.entrySet( ) ) {
      if ( params.containsKey( e.getKey( ) ) && !populateObjectField( obj, e, params ) ) {
        failedMappings.add( e.getKey( ) );
      } else {
        failedMappings.remove( e.getKey( ) );
      }
    }
    return failedMappings;
  }
  
  @SuppressWarnings( "unchecked" )
  private boolean populateObjectField( final GroovyObject obj, final Map.Entry<String, String> paramFieldPair, final Map<String, String> params ) {
    try {
      Class<?> declaredType = getRecursiveField( obj.getClass( ), paramFieldPair.getValue( ) ).getType( );
      if ( declaredType.equals( String.class ) )
        obj.setProperty( paramFieldPair.getValue( ), params.remove( paramFieldPair.getKey( ) ) );
      else if ( declaredType.getName( ).equals( "int" ) )
        obj.setProperty( paramFieldPair.getValue( ), Integer.parseInt( params.remove( paramFieldPair.getKey( ) ) ) );
      else if ( declaredType.equals( Integer.class ) )
        obj.setProperty( paramFieldPair.getValue( ), new Integer( params.remove( paramFieldPair.getKey( ) ) ) );
      else if ( declaredType.getName( ).equals( "boolean" ) )
        obj.setProperty( paramFieldPair.getValue( ), Boolean.parseBoolean( params.remove( paramFieldPair.getKey( ) ) ) );
      else if ( declaredType.equals( Boolean.class ) )
        obj.setProperty( paramFieldPair.getValue( ), new Boolean( params.remove( paramFieldPair.getKey( ) ) ) );
      else if ( declaredType.getName( ).equals( "long" ) )
        obj.setProperty( paramFieldPair.getValue( ), Long.parseLong( params.remove( paramFieldPair.getKey( ) ) ) );
      else if ( declaredType.equals( Long.class ) )
        obj.setProperty( paramFieldPair.getValue( ), new Long( params.remove( paramFieldPair.getKey( ) ) ) );
      else return false;
      return true;
    } catch ( Exception e1 ) {
      return false;
    }
  }
  
  @SuppressWarnings( "rawtypes" )
  private List<String> populateObjectList( final GroovyObject obj, final Map.Entry<String, String> paramFieldPair, final Map<String, String> params, final int paramSize ) {
    List<String> failedMappings = new ArrayList<String>( );
    try {
      Field declaredField = getRecursiveField( obj.getClass( ), paramFieldPair.getValue( ) );
      ArrayList theList = ( ArrayList ) obj.getProperty( paramFieldPair.getValue( ) );
      Class genericType = ( Class ) ( ( ParameterizedType ) declaredField.getGenericType( ) ).getActualTypeArguments( )[0];
      // :: simple case: FieldName.# :://
      if ( String.class.equals( genericType ) ) {
        if ( params.containsKey( paramFieldPair.getKey( ) ) ) {
          theList.add( params.remove( paramFieldPair.getKey( ) ) );
        } else {
          List<String> keys = Lists.newArrayList( params.keySet( ) );
          for ( String k : keys ) {
            if ( k.matches( paramFieldPair.getKey( ) + "\\.\\d*" ) ) {
              theList.add( params.remove( k ) );
            }
          }
        }
      } else if ( declaredField.isAnnotationPresent( HttpEmbedded.class ) ) {
        HttpEmbedded annoteEmbedded = ( HttpEmbedded ) declaredField.getAnnotation( HttpEmbedded.class );
        // :: build the parameter map and call populate object recursively :://
        if ( annoteEmbedded.multiple( ) ) {
          String prefix = paramFieldPair.getKey( );
          List<String> embeddedListFieldNames = new ArrayList<String>( );
          for ( String actualParameterName : params.keySet( ) )
            if ( actualParameterName.matches( prefix + ".1.*" ) ) embeddedListFieldNames.add( actualParameterName.replaceAll( prefix + ".1.", "" ) );
          for ( int i = 0; i < paramSize + 1; i++ ) {
            boolean foundAll = true;
            Map<String, String> embeddedParams = new HashMap<String, String>( );
            for ( String fieldName : embeddedListFieldNames ) {
              String paramName = prefix + "." + i + "." + fieldName;
              if ( !params.containsKey( paramName ) ) {
                failedMappings.add( "Mismatched embedded field: " + paramName );
                foundAll = false;
              } else embeddedParams.put( fieldName, params.get( paramName ) );
            }
            if ( foundAll )
              failedMappings.addAll( populateEmbedded( genericType, embeddedParams, theList ) );
            else break;
          }
        } else failedMappings.addAll( populateEmbedded( genericType, params, theList ) );
      }
    } catch ( Exception e1 ) {
      failedMappings.add( paramFieldPair.getKey( ) );
    }
    return failedMappings;
  }
  
  private List<String> populateEmbedded( final Class<?> genericType, final Map<String, String> params, @SuppressWarnings( "rawtypes" ) final ArrayList theList ) throws InstantiationException, IllegalAccessException {
    GroovyObject embedded = ( GroovyObject ) genericType.newInstance( );
    Map<String, String> embeddedFields = buildFieldMap( genericType );
    int startSize = params.size( );
    List<String> embeddedFailures = populateObject( embedded, embeddedFields, params );
    if ( embeddedFailures.isEmpty( ) && !( params.size( ) - startSize == 0 ) ) theList.add( embedded );
    return embeddedFailures;
  }
  
  private Map<String, String> buildFieldMap( Class<?> targetType ) {
    Map<String, String> fieldMap = new HashMap<String, String>( );
    while ( !BaseMessage.class.equals( targetType ) && !EucalyptusMessage.class.equals( targetType ) && !EucalyptusData.class.equals( targetType ) && !BaseData.class.equals( targetType ) ) {
      Field[] fields = targetType.getDeclaredFields( );
      for ( Field f : fields ) {
        if ( Modifier.isStatic( f.getModifiers( ) ) )
          continue;
        else if ( f.isAnnotationPresent( HttpParameterMapping.class ) ) {
          fieldMap.put( f.getAnnotation( HttpParameterMapping.class ).parameter( ), f.getName( ) );
          fieldMap.put( f.getName( ).substring( 0, 1 ).toUpperCase( ).concat( f.getName( ).substring( 1 ) ), f.getName( ) );
        } else {
          fieldMap.put( f.getName( ).substring( 0, 1 ).toUpperCase( ).concat( f.getName( ).substring( 1 ) ), f.getName( ) );
        }
      }
      targetType = targetType.getSuperclass( );
    }
    return fieldMap;
  }
  
}