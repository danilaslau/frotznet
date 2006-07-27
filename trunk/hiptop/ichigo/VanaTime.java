package org.cursetheyagudo.ichigo;

// (based on vanatime.c and vanatime.h, originally written by Victoria Lease)

class VanaTime
{
	public static long etimetovtime(long eclock) {
		return (eclock * 25) + 2312874000L;
	}
	
	public static long vtimetoetime(long vclock) {
		return (vclock - 2312874000L) / 25;
	}

	public VanaTime() {
		setToEarthTime();
	}
	
	public VanaTime(long vclock) {
		set(vclock);
	}
		
	public void setToEarthTime() {
		set(etimetovtime(System.currentTimeMillis() / 1000));
	}
	
	public void set(long vclock) {
			// split vtime into seconds / minutes / hours / days / months / years
		sec  = (int) (vclock % 60);
		min  = (int) ((vclock % (60 * 60)) / (60));
		hour = (int) ((vclock % (24 * 60 * 60)) / (60 * 60));
		mday = (int) (1 + (vclock % (30 * 24 * 60 * 60)) / (24 * 60 * 60));
		mon  = (int) (1 + ((vclock % (12 * 30 * 24 * 60 * 60)) / (30 * 24 * 60 * 60)));
		year = (int) (vclock / (12 * 30 * 24 * 60 * 60));
		
			// generate various versions of day
		yday = (int) ((vclock % (12 * 30 * 24 * 60 * 60)) / (24 * 60 * 60));
		wday = (int) (yday % 8);		
	}

	public long get() {
		long result = sec;
		result += min * (60);
		result += hour * (60 * 60);
		result += (mday - 1) * (24 * 60 * 60);
		result += (mon - 1) * (30 * 24 * 60 * 60);
		result += year * (12 * 30 * 24 * 60 * 60);
		return result;
	}

	public String timeString24() {
		temp[0] = (char) ('0' + (hour / 10));
		temp[1] = (char) ('0' + (hour % 10));
		temp[2] = ':';
		temp[3] = (char) ('0' + (min / 10));
		temp[4] = (char) ('0' + (min % 10));
		temp[5] = ':';
		temp[6] = (char) ('0' + (sec / 10));
		temp[7] = (char) ('0' + (sec % 10));
		return new String(temp);
	}
	
	String dayString() {
		return daynames[wday];
	}
	
	int sec;
	int min;
	int hour;
	int mday;
	int mon;
	int year;
	int yday;
	int wday;

	char temp[] = new char[8];
	
	private static final String[] daynames = {
		"Firesday",
		"Earthsday",
		"Watersday",
		"Windsday",
		"Icesday",
		"Thundersday",
		"Lightsday",
		"Darksday",
	};
}
