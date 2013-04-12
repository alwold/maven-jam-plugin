package edu.asu.wit.maven;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.util.Map;
import javax.xml.transform.TransformerException;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.settings.MavenSettingsBuilder;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.apache.xpath.XPathAPI;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.cyberneko.html.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 *
 * @author alwold
 */
public abstract class JamScrapeMojo extends AbstractMojo {

	/** @component */
	private MavenSettingsBuilder settingsBuilder;
	private String BASE_ACTION_URL = "https://webapp4.asu.edu/jam/";
	private static String authenticator;

	public void performJamAction(String appName, String action, Map<String, String> params) throws MojoExecutionException, MojoFailureException {
		HttpClient client = new HttpClient();
		if (authenticator == null) {
			authenticator = "";
		}
		PostMethod post = buildPostUrl(authenticator, appName, action, params);
		try {
			client.executeMethod(post);
			if (post.getResponseHeader("Location") != null && post.getResponseHeader("Location").getValue().contains("weblogin")) {
				post.releaseConnection();
				String[] postInfo = getLoginPostInfo(client, post.getResponseHeader("Location").getValue());
				String username;
				String password;
				Settings settings = settingsBuilder.buildSettings();
				if (settings.getServer("jam") == null) {
					getLog().info("Please enter your username:");
					BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
					username = br.readLine();
					password = new String(System.console().readPassword("Please enter your password:"));
				} else {
					Server server = settings.getServer("jam");
					username = server.getUsername();
					password = server.getPassword();
				}
				URI postURI = new URI(new URI(post.getResponseHeader("Location").getValue(), false), postInfo[0], false);
				PostMethod loginPost = new PostMethod(postURI.toString());
				System.out.println("login post goes to "+post.getResponseHeader("Location").getValue());
				loginPost.addParameter("username", username);
				loginPost.addParameter("password", password);
//				loginPost.addParameter("Login", "Login");
//				loginPost.addParameter("callapp", BASE_ACTION_URL);
				loginPost.addParameter("lt", postInfo[1]);
				loginPost.addParameter("_eventId", "submit");
				client.executeMethod(loginPost);
				authenticator = null;
				loginPost.releaseConnection();
				if (loginPost.getResponseHeader("location") != null) {
					System.out.println(loginPost.getResponseHeader("location").getValue());
					GetMethod casLogin = new GetMethod(loginPost.getResponseHeader("location").getValue());
					casLogin.setFollowRedirects(false);
					client.executeMethod(casLogin);
					casLogin.releaseConnection();
					System.out.println(casLogin.getResponseHeader("location").getValue());
					URL redir = new URL(casLogin.getResponseHeader("location").getValue());
					String[] pieces = redir.getQuery().split("&");
					for (String piece : pieces) {
						if (piece.startsWith("authenticator=")) {
							authenticator = piece.substring("authenticator=".length());
						}
					}
				} else {
					System.out.println("no redir");
				}
				if (authenticator != null) {
					post = buildPostUrl(authenticator, appName, action, params);
					client.executeMethod(post);
				} else {
					throw new MojoFailureException("Error getting authenticator");
				}
			}
			DOMParser parser = new DOMParser();
			parser.parse(new InputSource(post.getResponseBodyAsStream()));
			Document doc = parser.getDocument();
			String output = XPathAPI.selectSingleNode(doc, "//TABLE[@class='resultsTable']/TBODY/TR/TD/PRE").getTextContent();
			getLog().info(output);
			post.releaseConnection();
		} catch (IOException ex) {
			ex.printStackTrace();
			throw new MojoExecutionException("Unable to perform JAM action", ex);
		} catch (TransformerException ex) {
			ex.printStackTrace();
			throw new MojoExecutionException("Unable to parse return document", ex);
		} catch (SAXException ex) {
			ex.printStackTrace();
			throw new MojoExecutionException("Unable to parse return document", ex);
		} catch (XmlPullParserException e) {
			e.printStackTrace();
			throw new MojoExecutionException("Unable to load settings", e);
		}
	}

	private PostMethod buildPostUrl(String authenticator, String appName, String action, Map<String, String> params) {
		PostMethod post = new PostMethod(BASE_ACTION_URL + action);
		post.addParameter("project", appName);
		post.addParameter("type", "warapps");
		post.addParameter("authenticator", authenticator);
		for (String key : params.keySet()) {
			post.addParameter(key, params.get(key));
		}
		return post;
	}

	private String[] getLoginPostInfo(HttpClient client, String url) throws SAXException, IOException, TransformerException, MojoExecutionException {
		GetMethod get = new GetMethod(url);
		client.executeMethod(get);
		InputStream is = get.getResponseBodyAsStream();
		
		DOMParser parser = new DOMParser();
		parser.parse(new InputSource(is));
		Document doc = parser.getDocument();
		get.releaseConnection();
		Node node = XPathAPI.selectSingleNode(doc, "//INPUT[@name='lt']/@value");
		if (node == null) {
			throw new MojoExecutionException("Unable to find lt field in webauth login page");
		} else {
			String lt = node.getTextContent();
			node = XPathAPI.selectSingleNode(doc, "//FORM[@id='login']/@action");
			if (node == null) {
				throw new MojoExecutionException("Unable to find login form");
			} else {
				String postUrl = node.getTextContent();
				return new String[]{postUrl, lt};
			}
		}
	}
}
