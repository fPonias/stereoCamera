package com.munger.stereocamera.ip;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface Socket
{
	InputStream getInputStream() throws IOException;
	OutputStream getOutputStream() throws IOException;
	boolean isConnected();
}
