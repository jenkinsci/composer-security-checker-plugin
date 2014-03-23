package org.jenkinsci.plugins.composer_security_checker_plugin;
import hudson.Launcher;
import hudson.Extension;
import hudson.util.FormValidation;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.AbstractProject;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import net.sf.json.JSONObject;

import hudson.util.Scrambler;
import hudson.util.XStream2;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.QueryParameter;

import javax.servlet.ServletException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.ProxyHost;
import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;


/**
 * Sample {@link Builder}.
 *
 * <p>
 * When the user configures the project and enables this builder,
 * {@link DescriptorImpl#newInstance(StaplerRequest)} is invoked
 * and a new {@link HelloWorldBuilder} is created. The created
 * instance is persisted to the project configuration XML by using
 * XStream, so this allows you to use instance fields (like {@link #name})
 * to remember the configuration.
 *
 * <p>
 * When a build is performed, the {@link #perform(AbstractBuild, Launcher, BuildListener)}
 * method will be invoked. 
 *
 * @author Laurent RICHARD
 */
public class SecurityCheckerBuilder extends Builder {

    private final String apiUrl = "https://security.sensiolabs.org/check_lock";

    // proxy
    private String server ;
    private int port ;
    private String userName ;
    private String userPassword ;

    public String getUserName() {
        return userName;
    }
    public String getUserPassword() {
        return userPassword;
    }
    public int getPort() {
        return port;
    }
    public String getServer() {
        return server;
    }

    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public SecurityCheckerBuilder()
    {

    }

    private HttpClient getHttpClient() {

        this.server = getDescriptor().getProxyServer();
        this.port = getDescriptor().getProxyPort() ;
        this.userName = getDescriptor().getProxyUserName();
        this.userPassword = getDescriptor().getProxyUserPassword();

        HttpClient client = new HttpClient();

        
        // test si besoin proxy

        if( this.server != null && !this.server.isEmpty() ){

            HostConfiguration config = client.getHostConfiguration();
            config.setProxy( this.server, this.port );

            if( this.userName != null && !this.userName.isEmpty() && this.userPassword != null && !this.userPassword.isEmpty() ){
                
                Credentials credentials = new UsernamePasswordCredentials(this.userName, this.userPassword);
                AuthScope authScope = new AuthScope( this.server, this.port );

                client.getState().setProxyCredentials(authScope, credentials);
            }
        }
        //client.getHostConfiguration().setProxyHost(new ProxyHost(this.server, this.port));
            


        return client;

    }

    //public static Proxy createProxy( String name, int port) {
    //    return new Proxy(Proxy.Type.HTTP, new InetSocketAddress(name,port));
    //}

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener)throws FileNotFoundException {
        
    	listener.getLogger().println("============================================================" );
    	listener.getLogger().println("Composer Security Check : \n" );
    			
    	String resultUUid=null;
        
        
    	
        PostMethod postMethod = new PostMethod(this.apiUrl);
        //postMethod.setRequestHeader("Accept", "application/json");
        postMethod.setRequestHeader("Accept", "text/plain");
        //postMethod.setRequestHeader("Content-type", "multipart/form-data; charset=UTF8");
        

        // Credentials credentials = new UsernamePasswordCredentials(username, password);
        // client.getState().setCredentials(AuthScope.ANY, credentials);

        String composerLockFileFullPath = String.format("%s/composer.lock", build.getWorkspace());

        File fileComposerLock=new File( composerLockFileFullPath );

        //Part[] parts = {new FilePart(fileComposerLock.getName(), fileComposerLock)};
        Part[] parts = {new FilePart("lock", fileComposerLock)};
        postMethod.setRequestEntity(new MultipartRequestEntity(parts, postMethod.getParams()));

        //HttpClient client = new HttpClient();
        
        try {

            HttpClient client = getHttpClient();

            int status = client.executeMethod(postMethod);
            listener.getLogger().printf("composer file: %s%n", composerLockFileFullPath);
            //listener.getLogger().printf("Response code: %d%n", status);
            
            resultUUid = postMethod.getResponseBodyAsString();
            //listener.getLogger().printf("resultUUid : %s%n", resultUUid);
        } catch (HttpException e) {
            e.printStackTrace(listener.error("Unable to notify API URL %s",  this.apiUrl));
        } catch (IOException e) {
            e.printStackTrace(listener.error("Unable to connect to API URL %s", this.apiUrl));
        } finally {
        	
            postMethod.releaseConnection();
        }
        // display return
        listener.getLogger().printf("%s", resultUUid );
        listener.getLogger().println("============================================================" );
    	
        if( resultUUid.contains("No known")){
            return true;
        }else{  
            return false;
        }
          //return true; 
    }


    // Overridden for better type safety.
    // If your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    /**
     * Descriptor for {@link HelloWorldBuilder}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     *
     * <p>
     * See <tt>src/main/resources/hudson/plugins/hello_world/HelloWorldBuilder/*.jelly</tt>
     * for the actual HTML fragment for the configuration screen.
     */
    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        /**
         * To persist global configuration information,
         * simply store it in a field and call save().
         *
         * <p>
         * If you don't want fields to be persisted, use <tt>transient</tt>.
         */
        //private boolean useFrench;
        // proxy
        private  String server ;
        private  int port ;
        private  String userName ;
        private  String userPassword ;

        /**
         * In order to load the persisted global configuration, you have to 
         * call load() in the constructor.
         */
        public DescriptorImpl() {
            load();
        }

        /**
         * Performs on-the-fly validation of the form field 'name'.
         *
         * @param value
         *      This parameter receives the value that the user has typed.
         * @return
         *      Indicates the outcome of the validation. This is sent to the browser.
         */
        /*public FormValidation doCheckName(@QueryParameter String value)
                throws IOException, ServletException {
            //if (value.length() == 0)
            //    return FormValidation.error("Please set a name");
            //if (value.length() < 4)
            //    return FormValidation.warning("Isn't the name too short?");
            return FormValidation.ok();
        }*/

        public FormValidation doCheckPort(@QueryParameter String value) {
            //value = Util.fixEmptyAndTrim(value);
            value = value;
            if (value == null) {
                return FormValidation.ok();
            }
            int port;
            try {
                port = Integer.parseInt(value);
            } catch (NumberFormatException e) {
                return FormValidation.error("Messages.PluginManager_PortNotANumber()");
            }
            if (port < 0 || port > 65535) {
                return FormValidation.error("Messages.PluginManager_PortNotInRange(0, 65535)");
            }
            return FormValidation.ok();
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types 
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return "Composer Security Checker";
        }

        public String getProxyServer() {
            return server;
        }
        public int getProxyPort() {
            return port;
        }
        public String getProxyUserName() {
            return userName;
        }
        public String getProxyUserPassword() {
            return userPassword;
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            // To persist global configuration information,
            // set that to properties and call save().
            server = formData.getString("server");
            port = formData.getInt("port");
            userName = formData.getString("userName");
            userPassword = formData.getString("userPassword");
            // ^Can also use req.bindJSON(this, formData);
            //  (easier when there are many fields; need set* methods for this, like setUseFrench)
            save();
            return super.configure(req,formData);
        }

        /*private static final XStream XSTREAM = new XStream2();

        public static XmlFile getXmlFile() {
            return new XmlFile(XSTREAM, new File(Jenkins.getInstance().getRootDir(), "proxy.xml"));
        }

        public void save() throws IOException {
            if(BulkChange.contains(this))   return;
            XmlFile config = getXmlFile();
            config.write(this);
            SaveableListener.fireOnChange(this, config);
        }*/

    }
}

