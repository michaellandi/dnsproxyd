/*
 * dnsproxyd
 * Version 1.0
 * Copyright © 2008 Michael Landi
 *
 * This file is part of dnsproxyd.
 *
 * Dnsproxyd is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Dnsproxyd is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Dnsproxyd.  If not, see <http://www.gnu.org/licenses/>
 */

import java.net.*;
import java.util.*;

public class EventListener extends Thread {
	private int			_intPort;
	private boolean		_varListen;
	private DNSProxy		_dnspPointer;

	public EventListener(DNSProxy dnspPointer, int port) {
		_intPort = port;
		_dnspPointer = dnspPointer;
	}

	public void close() {
		_varListen = false;
	}

	public void run() {
		try {
			listen();
		}
		catch (Exception e) {
			DNSProxy.printDebug(e);
		}
	}

	private void listen() throws Exception {
		_varListen = true;
		byte[] bBuffer = new byte[4];
		DatagramSocket dSocket = new DatagramSocket(_intPort);
		DatagramPacket dPacket;

		while (_varListen) {
			dPacket = new DatagramPacket(bBuffer, bBuffer.length);
			dSocket.receive(dPacket);

			if (dPacket.getAddress().getHostAddress().equals("127.0.0.1")) {
				if (bBuffer[0] == 24)
					if (bBuffer[1] == 48)
						_dnspPointer.close();
			}
		}

		dSocket.close();
	}
}
