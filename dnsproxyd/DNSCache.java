/*
 * dnsproxyd
 * Version 1.0
 * Copyright � 2008 Michael Landi
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


import java.util.HashMap;
import java.util.Map;

public class DNSCache {
	/*
	 * Global variables.
	 */
	private Map<String, DNSEntry>		_vecCache;

	/*
	 * Default constructor, accepts no arguments.
	 */
	public DNSCache() {
		_vecCache = new HashMap<String, DNSEntry>();
	}

	public DNSEntry isCached(String strDomain, int _intCacheTime) {
		strDomain = strDomain.trim().toLowerCase();

		DNSEntry dnsEntry = _vecCache.get(strDomain); 
		if (dnsEntry == null) {
			return null;
		}
		if ((System.currentTimeMillis() - _intCacheTime) > dnsEntry.getDate()) {
			_vecCache.remove(strDomain);
			return null;
		}
		return dnsEntry;
	}

	public void cache(String domain, byte[] address) {
		domain = domain.trim().toLowerCase();

		if (! _vecCache.containsKey(domain))
			_vecCache.put(domain, new DNSEntry(domain, address));
	}

	public int prune(long time) {
		int intCount = 0;

		for (String hostname: _vecCache.keySet()) {
			if ((System.currentTimeMillis() - time) > _vecCache.get(hostname).getDate()) {
				_vecCache.remove(hostname);
				intCount++;
			}
		}

		return intCount;
	}

	public int size() {
		return _vecCache.size();
	}
}
