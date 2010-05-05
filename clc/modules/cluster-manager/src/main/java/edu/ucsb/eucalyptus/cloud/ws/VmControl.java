/*******************************************************************************
 *Copyright (c) 2009  Eucalyptus Systems, Inc.
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
 *    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************/
/*
 *
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */

package edu.ucsb.eucalyptus.cloud.ws;

import java.util.NoSuchElementException;
import java.util.Date;
import org.apache.log4j.Logger;
import org.mule.RequestContext;
import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.cluster.callback.BundleCallback;
import com.eucalyptus.cluster.callback.CancelBundleCallback;
import com.eucalyptus.cluster.callback.PasswordDataCallback;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.ws.util.Messaging;
import com.eucalyptus.auth.Users;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.NoSuchUserException;
import edu.ucsb.eucalyptus.cloud.VmAllocationInfo;
import edu.ucsb.eucalyptus.cloud.cluster.VmInstance;
import edu.ucsb.eucalyptus.cloud.cluster.VmInstances;
import edu.ucsb.eucalyptus.cloud.entities.SystemConfiguration;
import com.eucalyptus.records.EventType;
import com.eucalyptus.vm.VmState;
import edu.ucsb.eucalyptus.msgs.BundleInstanceResponseType;
import edu.ucsb.eucalyptus.msgs.BundleInstanceType;
import edu.ucsb.eucalyptus.msgs.BundleTask;
import edu.ucsb.eucalyptus.msgs.CancelBundleTaskResponseType;
import edu.ucsb.eucalyptus.msgs.CancelBundleTaskType;
import edu.ucsb.eucalyptus.msgs.DescribeBundleTasksResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeBundleTasksType;
import edu.ucsb.eucalyptus.msgs.DescribeInstancesResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeInstancesType;
import edu.ucsb.eucalyptus.msgs.EucalyptusErrorMessageType;
import edu.ucsb.eucalyptus.msgs.EventRecord;
import edu.ucsb.eucalyptus.msgs.GetConsoleOutputType;
import edu.ucsb.eucalyptus.msgs.GetPasswordDataResponseType;
import edu.ucsb.eucalyptus.msgs.GetPasswordDataType;
import edu.ucsb.eucalyptus.msgs.RebootInstancesResponseType;
import edu.ucsb.eucalyptus.msgs.RebootInstancesType;
import edu.ucsb.eucalyptus.msgs.TerminateInstancesResponseType;
import edu.ucsb.eucalyptus.msgs.TerminateInstancesType;

public class VmControl {
  
  private static Logger LOG = Logger.getLogger( VmControl.class );
  
  public VmAllocationInfo allocate( VmAllocationInfo vmAllocInfo ) throws EucalyptusCloudException {
    return vmAllocInfo;
  }
  
  public DescribeInstancesResponseType DescribeInstances( DescribeInstancesType msg ) throws EucalyptusCloudException {
    DescribeInstancesResponseType reply = ( DescribeInstancesResponseType ) msg.getReply( );
    try {
      reply.setReservationSet( SystemState.handle( msg.getUserId( ), msg.getInstancesSet( ), msg.isAdministrator( ) ) );
    } catch ( Exception e ) {
      LOG.error( e );
      LOG.debug( e, e );
      throw new EucalyptusCloudException( e.getMessage( ) );
    }
    return reply;
  }
  
  public TerminateInstancesResponseType TerminateInstances( TerminateInstancesType msg ) throws EucalyptusCloudException {
    TerminateInstancesResponseType reply = ( TerminateInstancesResponseType ) msg.getReply( );
    try {
      return SystemState.handle( msg );
    } catch ( Exception e ) {
      LOG.error( e );
      LOG.debug( e, e );
      throw new EucalyptusCloudException( e.getMessage( ) );
    }
  }
  
  public RebootInstancesResponseType RebootInstances( RebootInstancesType msg ) throws EucalyptusCloudException {
    try {
      return SystemState.handle( msg );
    } catch ( Exception e ) {
      LOG.error( e );
      LOG.debug( e, e );
      throw new EucalyptusCloudException( e.getMessage( ) );
    }
  }
  
  public void GetConsoleOutput( GetConsoleOutputType request ) throws EucalyptusCloudException {
    try {
      SystemState.handle( request );
      RequestContext.getEventContext( ).setStopFurtherProcessing( true );
    } catch ( Exception e ) {
      LOG.error( e );
      LOG.debug( e, e );
      throw new EucalyptusCloudException( e.getMessage( ) );
    }
  }
  
  public DescribeBundleTasksResponseType describeBundleTasks( DescribeBundleTasksType request ) throws EucalyptusCloudException {
    DescribeBundleTasksResponseType reply = request.getReply( );
    if ( request.getBundleIds( ).isEmpty( ) ) {
      for ( VmInstance v : VmInstances.getInstance( ).listValues( ) ) {
        if ( v.isBundling( ) && ( request.isAdministrator( ) || v.getOwnerId( ).equals( request.getUserId( ) ) ) ) {
          reply.getBundleTasks( ).add( v.getBundleTask( ) );
        }
      }
      for ( VmInstance v : VmInstances.getInstance( ).listDisabledValues( ) ) {
        if ( v.isBundling( ) && ( request.isAdministrator( ) || v.getOwnerId( ).equals( request.getUserId( ) ) ) ) {
          reply.getBundleTasks( ).add( v.getBundleTask( ) );
        }
      }
    } else {
      for( String bundleId : request.getBundleIds( ) ) {
        try {
          VmInstance v = VmInstances.getInstance( ).lookupByBundleId( bundleId );
          if ( v.isBundling( ) && ( request.isAdministrator( ) || v.getOwnerId( ).equals( request.getUserId( ) ) ) ) {
            reply.getBundleTasks( ).add( v.getBundleTask( ) );
          }
        } catch ( NoSuchElementException e ) {
        }
      }
    }
    return reply;
  }
  
  public CancelBundleTaskResponseType cancelBundleTask( CancelBundleTaskType request ) throws EucalyptusCloudException {
    CancelBundleTaskResponseType reply = request.getReply( );
    reply.set_return( true );
    
    try {
      VmInstance v = VmInstances.getInstance( ).lookupByBundleId( request.getBundleId( ) );
      if ( request.isAdministrator( ) || v.getOwnerId( ).equals( request.getUserId( ) ) ) {
        v.getBundleTask( ).setState( "canceling" );
        LOG.info( EventRecord.here( BundleCallback.class, EventType.BUNDLE_CANCELING, request.getUserId( ), v.getBundleTask( ).getBundleId( ), v.getInstanceId( ) ) );
        
	Cluster cluster = Clusters.getInstance( ).lookup( v.getPlacement( ) );

	request.setInstanceId(v.getInstanceId());
	reply.setTask(v.getBundleTask());
	new CancelBundleCallback(request).dispatch(cluster);

	return reply;
      } else {
        throw new EucalyptusCloudException( "Failed to find bundle task: " + request.getBundleId( ) );
      }
    } catch ( NoSuchElementException e ) {
      throw new EucalyptusCloudException( "Failed to find bundle task: " + request.getBundleId( ) );
    }
  }
  
  public BundleInstanceResponseType bundleInstance( BundleInstanceType request ) throws EucalyptusCloudException {
    BundleInstanceResponseType reply = request.getReply( );//TODO: check if the instance has platform windows.
    reply.set_return( true );
    String walrusUrl = SystemConfiguration.getWalrusUrl( );
    String instanceId = request.getInstanceId( );
    User user = null;
    try {
      user = Users.lookupUser( request.getUserId( ) );
    } catch ( NoSuchUserException e1 ) {
      throw new EucalyptusCloudException( "Failed to lookup the specified user's information: " + request.getUserId( ) );
    }
    try {
      VmInstance v = VmInstances.getInstance( ).lookup( instanceId );
      if( v.isBundling( ) ) {
        reply.setTask( v.getBundleTask( ) );
        return reply;
      } else if( !VmState.RUNNING.equals( v.getState( ) ) ) {
        throw new EucalyptusCloudException( "Failed to bundle requested vm because it is not currently 'running': " + request.getInstanceId( ) );
      } else if ( request.isAdministrator( ) || v.getOwnerId( ).equals( request.getUserId( ) ) ) {
        BundleTask bundleTask = new BundleTask( v.getInstanceId( ).replaceFirst( "i-", "bun-" ), v.getInstanceId( ), request.getBucket( ), request.getPrefix( ) );
        if ( v.startBundleTask( bundleTask ) ) {
          reply.setTask( bundleTask );
        } else if ( v.getBundleTask( ) == null ) {
          v.resetBundleTask( );
          v.startBundleTask( bundleTask );
          reply.setTask( bundleTask );
        } else {
          throw new EucalyptusCloudException( "Instance is already being bundled: " + v.getBundleTask( ).getBundleId( ) );
        }
        LOG.info( EventRecord.here( BundleCallback.class, EventType.BUNDLE_PENDING, request.getUserId( ), v.getBundleTask( ).getBundleId( ), v.getInstanceId( ) ) );
        BundleCallback callback = new BundleCallback( request );
        request.setUrl( walrusUrl );
        request.setAwsAccessKeyId( user.getQueryId( ) );
        callback.dispatch( v.getPlacement( ) );
        return reply;
      } else {
        throw new EucalyptusCloudException( "Failed to find instance: " + request.getInstanceId( ) );
      }
    } catch ( NoSuchElementException e ) {
      throw new EucalyptusCloudException( "Failed to find instance: " + request.getInstanceId( ) );
    }
  }

  public static void handle( GetPasswordDataType request ) throws Exception {
    try {
      Cluster cluster = null;
      VmInstance v = VmInstances.getInstance( ).lookup( request.getInstanceId( ) );
      if ( !VmState.RUNNING.equals( v.getState( ) ) ) {
        throw new NoSuchElementException( "Instance " + request.getInstanceId( ) + " is not in a running state." );
      }
      if ( request.isAdministrator( ) || v.getOwnerId( ).equals( request.getUserId( ) ) ) {
        cluster = Clusters.getInstance( ).lookup( v.getPlacement( ) );
      } else {
        throw new NoSuchElementException( "Instance " + request.getInstanceId( ) + " does not exist." );
      }
      RequestContext.getEventContext( ).setStopFurtherProcessing( true );
      if( v.getPasswordData( ) == null) {
        new PasswordDataCallback( request ).dispatch( cluster );
      } else { 
        GetPasswordDataResponseType reply = request.getReply( );
        reply.set_return( true );
        reply.setOutput( v.getPasswordData( ) );
        reply.setTimestamp( new Date() );
        reply.setInstanceId( v.getInstanceId( ) );
        Messaging.dispatch( "vm://ReplyQueue", reply );        
      }
    } catch ( NoSuchElementException e ) {
      Messaging.dispatch( "vm://ReplyQueue", new EucalyptusErrorMessageType( RequestContext.getEventContext( ).getService( ).getComponent( ).getClass( )
                                                                                           .getSimpleName( ), request, e.getMessage( ) ) );
    }
  }
  
}
