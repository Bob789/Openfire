/**
 * $Revision: 1722 $
 * $Date: 2005-07-28 15:19:16 -0700 (Thu, 28 Jul 2005) $
 *
 * Copyright (C) 2005-2008 Jive Software. All rights reserved.
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

package org.jivesoftware.openfire.plugin.rest;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.dom4j.Element;
import org.jivesoftware.database.DbConnectionManager;
import org.jivesoftware.openfire.container.Plugin;
import org.jivesoftware.openfire.container.PluginManager;
import org.jivesoftware.openfire.interceptor.InterceptorManager;
import org.jivesoftware.openfire.interceptor.PacketInterceptor;
import org.jivesoftware.openfire.interceptor.PacketRejectedException;
import org.jivesoftware.openfire.session.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.Message;
import org.xmpp.packet.Packet;
import org.xmpp.packet.PacketExtension;

public class GameCenterPlugin implements Plugin, PacketInterceptor {
	
	private static Logger LOG = LoggerFactory.getLogger(GameCenterPlugin.class);
	
	/** The Constant INSTANCE. */
	public static final GameCenterPlugin INSTANCE = new GameCenterPlugin();
	
	private InterceptorManager interceptorManager;
	
	private static final String ADD_STEP = "INSERT INTO tictactoe (loser, winner, step, posX, posY) VALUES(?, ?, ?, ?, ?)";
	
	private Connection con;
	
	private int step = 0;

	public static GameCenterPlugin getInstance() {
		return INSTANCE;
	}
	
	public GameCenterPlugin() {
		interceptorManager = InterceptorManager.getInstance();
	}

	/* (non-Javadoc)
	 * @see org.jivesoftware.openfire.container.Plugin#initializePlugin(org.jivesoftware.openfire.container.PluginManager, java.io.File)
	 */
	public void initializePlugin(PluginManager manager, File pluginDirectory) {
		LOG.error("initializePlugin of GameCenterPlugin");
//		Add this plugins to interceptors.
		interceptorManager.addInterceptor(this);
	}

	/* (non-Javadoc)
	 * @see org.jivesoftware.openfire.container.Plugin#destroyPlugin()
	 */
	public void destroyPlugin() {
		interceptorManager.removeInterceptor(this);
	}

	@Override
	public void interceptPacket(Packet packet, Session session,
			boolean incoming, boolean processed) throws PacketRejectedException {
		
//		If the packet is already processed, skip it
		if(processed) {
			return;
		}
		
		if(packet instanceof Message) {
			PacketExtension extension = packet.getExtension("TicTacToe", "http://www.kidslearning4fun.com/");
			
			if(extension != null) {
				if(extension.getElement().getName().equals("TicTacToe")) {
					Element rootElement = extension.getElement();
					
					Element elementX = rootElement.element("positionX");
					if(elementX != null) {
						LOG.error("positionX: " + elementX.getStringValue());
					}
					
					Element elementY = rootElement.element("positionY");
					if(elementY != null) {
						LOG.error("positionY: " + elementY.getStringValue());
					}
					
					Element elementStatus = rootElement.element("statusWin");
					if(elementStatus != null) {
						LOG.error("statusWin: " + elementStatus.getStringValue());
					}
					
					// Increase our steps
					step++;
					
					String from = packet.getFrom().toString().substring(0, packet.getFrom().toString().indexOf("@"));
					String to = packet.getTo().toString().substring(0, packet.getTo().toString().indexOf("@"));
					
					addGameStep(from, to, step, Integer.parseInt(elementX.getStringValue()), Integer.parseInt(elementY.getStringValue()));
				}
			}
		}
	}
	
	private void addGameStep(String from, String to, int step, int posX, int posY) {
		PreparedStatement pstmt = null;
		try {
			con = DbConnectionManager.getConnection();
			pstmt = con.prepareStatement(ADD_STEP);
			pstmt.setString(1, from);
			pstmt.setString(2, to);
			pstmt.setInt(3, step);
			pstmt.setInt(4, posX);
			pstmt.setInt(5, posY);
			pstmt.executeUpdate();
		} catch (SQLException e) {
			LOG.error("ERROR: " , e);
		} finally {
			DbConnectionManager.closeConnection(pstmt, con);
		}
	}
}
