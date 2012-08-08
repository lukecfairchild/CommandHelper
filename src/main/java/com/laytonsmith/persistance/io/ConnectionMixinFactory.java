package com.laytonsmith.persistance.io;

import com.laytonsmith.persistance.DataSource;
import com.laytonsmith.persistance.DataSourceException;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A ConnectionMixin class dictates how a data source connects to its data.
 * This can vary depending on the URI, and so this class grabs the appropriate
 * mixin based on the original URI.
 * @author lsmith
 */
public class ConnectionMixinFactory {
	private ConnectionMixinFactory(){}
	
	public static class ConnectionMixinOptions{
		File workingDirectory = null;
		/**
		 * In the case of file based connections, this is the working
		 * directory, that is, the "." directory used to resolve
		 * relative paths.
		 * @param workingDirectory 
		 */
		public void setWorkingDirectory(File workingDirectory){
			this.workingDirectory = workingDirectory;
		}		
		
	}
	
	/**
	 * A ConnectionMixin class dictates how a data source connects to its data.
	 * This can vary depending on the URI, and so this class grabs the appropriate
	 * mixin based on the original URI. It isn't always the case that a connection mixin
	 * will exist for this URI, if the data source implicitely provides it's own
	 * connection, it won't need this. This is generally the case for non-string
	 * based connections. If the data source does provide it's own connection information,
	 * this should be ignored, because it will probably not return the correct type
	 * anyways.
	 * @param uri
	 * @param modifiers
	 * @return 
	 */
	public static ConnectionMixin GetConnectionMixin(URI uri, Set<DataSource.DataSourceModifier> modifiers, ConnectionMixinOptions options, String blankDataModel) throws DataSourceException{
		if(modifiers.contains(DataSource.DataSourceModifier.HTTP) || modifiers.contains(DataSource.DataSourceModifier.HTTPS)){
			try {
				//This is a WebConnection
				return new WebConnection(uri);
			} catch (MalformedURLException ex) {
				throw new DataSourceException("Malformed URL.", ex);
			}
		} else if(modifiers.contains(DataSource.DataSourceModifier.SSH)){
			//This is an SSHConnection
			return new SSHConnection(uri);
		} else {
			//Else it's a file connection, or null, but we will go ahead
			//and assume it's file.
			if(modifiers.contains(DataSource.DataSourceModifier.READONLY)){
				return new ReadOnlyFileConnection(uri, options.workingDirectory, blankDataModel);
			} else {
				return new ReadWriteFileConnection(uri, options.workingDirectory, blankDataModel);
			}
		}
	}
}
