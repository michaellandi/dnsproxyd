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

import java.io.*;
import java.text.*;
import java.util.*;

public class DNSLog {
	private final int		BUFFSIZE	=	1750;

	private FileWriter		_fWriter;
	private BufferedWriter	_bWriter;
	private SimpleDateFormat	_sdfDate;
	private String			_strFilename;
	private int			_intDay;
	private Date			_dteLog;

	public DNSLog(String logpath) {
		_dteLog = new Date();
		_intDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
		_strFilename = logpath;
		_sdfDate = new SimpleDateFormat("hh':'mm':'ss");

		openHandle();
	}

	public void checkDate() {
		if (Calendar.getInstance().get(Calendar.DAY_OF_MONTH) != _intDay) {
			closeHandle();
			openHandle();
			_intDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
		}
	}

	public void print(String a) {
		checkDate();

		try {
			_bWriter.write(a);
		}
		catch (Exception e) {
			DNSProxy.printDebug(e );
		}
	}

	public void println(String a) {
		checkDate();

		try {
			_bWriter.write(a + 
				String.format(" %10s", _sdfDate.format(new Date())) + "\n");
		}
		catch (Exception e) {
			DNSProxy.printDebug(e);
		}
	}

	private void openHandle() {
		SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy'-'MM'-'dd");
		File fInfo = new File(_strFilename + "/" + sdfDate.format(_dteLog));

		try {
			_fWriter = new FileWriter(fInfo, true);
			_bWriter = new BufferedWriter(_fWriter, BUFFSIZE);
		}
		catch (Exception e) {
			DNSProxy.printDebug(e);
		}
	}

	public void closeHandle() {
		try {
			_bWriter.close();
			_fWriter.close();
		}
		catch (Exception e) {
			//Don't do anything here...dnsproxy is arleady closed.
		}
	}
}
