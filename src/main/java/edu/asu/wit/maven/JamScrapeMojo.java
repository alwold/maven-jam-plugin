/*
 */

package edu.asu.wit.maven;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Map;
import javax.xml.transform.TransformerException;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.xpath.XPathAPI;
import org.cyberneko.html.parsers.DOMParser;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 *
 * @author alwold
 */
public abstract class JamScrapeMojo extends AbstractMojo {
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
				// TODO figure out a better way to deal with this
				getLog().info("Please enter your username:");
				BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
				String username = br.readLine();
				getLog().info("Please enter your password:");
				String password = br.readLine();
				PostMethod loginPost = new PostMethod(post.getResponseHeader("Location").getValue());
				loginPost.addParameter("form", "login");
				loginPost.addParameter("userid", username);
				loginPost.addParameter("password", password);
				loginPost.addParameter("Login", "Login");
				loginPost.addParameter("callapp", BASE_ACTION_URL);
				post.releaseConnection();
				client.executeMethod(loginPost);
				authenticator = null;
				if (loginPost.getResponseHeader("location") != null) {
					URL redir = new URL(loginPost.getResponseHeader("location").getValue());
					String[] pieces = redir.getQuery().split("&");
					for (String piece: pieces) {
						if (piece.startsWith("authenticator=")) {
							authenticator = piece.substring("authenticator=".length());
						}
					}
				}
				loginPost.releaseConnection();
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
		}
	}

	private PostMethod buildPostUrl(String authenticator, String appName, String action, Map<String, String> params) {
		PostMethod post = new PostMethod(BASE_ACTION_URL+action);
		post.addParameter("project", appName);
		post.addParameter("type", "warapps");
		post.addParameter("authenticator", authenticator);
		for (String key: params.keySet()) {
			post.addParameter(key, params.get(key));
		}
		return post;
	}

}
