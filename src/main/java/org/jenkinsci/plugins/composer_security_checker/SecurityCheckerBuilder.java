package org.jenkinsci.plugins.composer_security_checker;
import hudson.Launcher;
import hudson.Extension;
import hudson.util.FormValidation;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.AbstractProject;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.QueryParameter;

import javax.servlet.ServletException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;

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

    private final String name;
    private final String apimode;
    //private final String name;
    private final String apiUrl = "https://security.sensiolabs.org/check_lock";

    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public SecurityCheckerBuilder(String apimode) {
        this.name = "test";
        this.apimode = apimode; // "API"; //"LOCAL"
    }

    /**
     * We'll use this from the <tt>config.jelly</tt>.
     */
    //public String getName() {
    //    return name;
    //}
    
    public String getApimode() {
        return apimode;
    }

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

        HttpClient client = new HttpClient();

        try {
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
        private boolean useFrench;

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
        public FormValidation doCheckName(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("Please set a name");
            if (value.length() < 4)
                return FormValidation.warning("Isn't the name too short?");
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

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            // To persist global configuration information,
            // set that to properties and call save().
            useFrench = formData.getBoolean("useFrench");
            // ^Can also use req.bindJSON(this, formData);
            //  (easier when there are many fields; need set* methods for this, like setUseFrench)
            save();
            return super.configure(req,formData);
        }

        /**
         * This method returns true if the global configuration says we should speak French.
         *
         * The method name is bit awkward because global.jelly calls this method to determine
         * the initial state of the checkbox by the naming convention.
         */
        public boolean getUseFrench() {
            return useFrench;
        }
    }
}

