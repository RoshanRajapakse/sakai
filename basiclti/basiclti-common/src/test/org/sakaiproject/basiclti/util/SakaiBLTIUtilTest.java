/**
 * Copyright (c) 2009-2017 The Apereo Foundation
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *             http://opensource.org/licenses/ecl2
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sakaiproject.basiclti.util;

import static org.junit.Assert.assertEquals;

import lombok.extern.slf4j.Slf4j;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.util.Map;
import java.util.TreeMap;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.json.simple.JSONObject;

import org.sakaiproject.lti.api.LTIService;
import org.sakaiproject.basiclti.util.SakaiBLTIUtil;
import org.tsugi.basiclti.BasicLTIUtil;
import org.tsugi.basiclti.BasicLTIConstants;
import org.tsugi.lti13.LTI13ConstantsUtil;

@Slf4j
public class SakaiBLTIUtilTest {

	public static String [] shouldBeTheSame = {
		null,
		"",
		"     ",
		" \n \n",
		"42",
		"x=1",
		"x=1;",
		"x=1;\nx=2;",
		"x=1; ",
		"x=1;y=2;99;z=3", // Can have only 1 semicolon between = signs
		"x=1;42",
		"x=1;y=2z=3;",
		"x;19=1;42",
		"x=1\ny=2\nz=3",
		"x=1;\ny=2\nz=3"
	};

	public static Set<String> projectRoles = Set.<String>of("access", "maintain");
	public static Set<String> courseRoles = Set.<String>of("Student", "Instructor", "Teaching Assistant"); // Keep the blank!
	public static Set<String> ltiRoles = Set.<String>of("Instructor", "Teaching Assistant",
		"ContentDeveloper", "Faculty", "Member", "Learner", "Mentor", "Staff", "Alumni", "ProspectiveStudent", "Guest",
		"Other", "Administrator", "Manager", "Observer", "Officer", "None"
	);

	@Before
	public void setUp() throws Exception {
	}

	/**
         * If it is null, blank, or has no equal signs return unchanged
         * If there is one equal sign return unchanged
         * If there is a new line anywhere in the string after trim, return unchanged
         * If we see ..=..;..=..;..=..[;] - we replace ; with \n
	 */

	@Test
	public void testStrings() {
		String adj = null;
		for(String s: shouldBeTheSame) {
			adj = SakaiBLTIUtil.adjustCustom(s);
			assertEquals(s, adj);
		}

		adj = SakaiBLTIUtil.adjustCustom("x=1;y=2;z=3");
		assertEquals(adj,"x=1;y=2;z=3".replace(';','\n'));
		adj = SakaiBLTIUtil.adjustCustom("x=1;y=2;z=3;");
		assertEquals(adj,"x=1;y=2;z=3;".replace(';','\n'));
	}
	@Test
	public void testStringGrade() {
		String grade="";
		try {
			grade = SakaiBLTIUtil.getRoundedGrade(0.57,100.0);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			log.error(e.getMessage(), e);
		}

		assertEquals(grade,"57.0");

		try {
			grade = SakaiBLTIUtil.getRoundedGrade(0.5655,100.0);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			log.error(e.getMessage(), e);
		}

		assertEquals(grade,"56.55");
	}

	// Something like: 4bd442a8b27e647e:2803e729800336b20a77d61b2da6db3f:790b8098f8bb4407f96304e701eeb58e:AES/CBC/PKCS5Padding
	// But each encryption is distinct
	public boolean goodEncrypt(String enc) {
		String [] pieces = enc.split(":");
		if ( pieces.length != 4 ) {
			System.out.println("Bad encryption - too few pieces\n"+enc);
			return false;
		}
		if ( ! "AES/CBC/PKCS5Padding".equals(pieces[3]) ) {
			System.out.println("Bad encryption - must end with AES/CBC/PKCS5Padding\n"+enc);
			return false;
		}
		return true;
	}

	@Test
	public void testEncryptDecrypt() {
		String plain = "plain";
		String key = "bob";
		String encrypt1 = SakaiBLTIUtil.encryptSecret(plain, key);
		assertFalse(plain.equals(encrypt1));
		assertTrue(goodEncrypt(encrypt1));
		// No double encrypt
		String encrypt2 = SakaiBLTIUtil.encryptSecret(encrypt1, key);
		assertTrue(goodEncrypt(encrypt2));
		assertEquals(encrypt1, encrypt2);
		boolean checkonly = false;
		String decrypt = SakaiBLTIUtil.decryptSecret(encrypt2, key, checkonly);
		assertEquals(plain, decrypt);
	}

	@Test
	public void testLaunchCodes() {
		Map<String, Object> content = new TreeMap<String, Object>();
		content.put(LTIService.LTI_ID, "42");
		content.put(LTIService.LTI_PLACEMENTSECRET, "xyzzy");

		String launch_code_key = SakaiBLTIUtil.getLaunchCodeKey(content);
		assertEquals(launch_code_key,"launch_code:42");

		String launch_code = SakaiBLTIUtil.getLaunchCode(content);
		assertTrue(SakaiBLTIUtil.checkLaunchCode(content, launch_code));

		content.put(LTIService.LTI_PLACEMENTSECRET, "wrong");
		assertFalse(SakaiBLTIUtil.checkLaunchCode(content, launch_code));

		// Correct password different id
		content.put(LTIService.LTI_ID, "43");
		content.put(LTIService.LTI_PLACEMENTSECRET, "xyzzy");
		assertFalse(SakaiBLTIUtil.checkLaunchCode(content, launch_code));
	}

	@Test
	public void testConvertLong() {
		Long l = SakaiBLTIUtil.getLongNull(new Long(2));
		assertEquals(l, new Long(2));
		l = SakaiBLTIUtil.getLongNull(new Double(2.2));
		assertEquals(l, new Long(2));
		l = SakaiBLTIUtil.getLongNull(null);
		assertEquals(l, null);
		l = SakaiBLTIUtil.getLongNull("fred");
		assertEquals(l, null);
		l = SakaiBLTIUtil.getLongNull("null");
		assertEquals(l, null);
		l = SakaiBLTIUtil.getLongNull("NULL");
		assertEquals(l, null);
		// This one is a little weird but it is how it was written - double is different
		l = SakaiBLTIUtil.getLongNull("");
		assertEquals(l, new Long(-1));
		l = SakaiBLTIUtil.getLongNull("2");
		assertEquals(l, new Long(2));
		l = SakaiBLTIUtil.getLongNull("2.5");
		assertEquals(l, null);
		l = SakaiBLTIUtil.getLongNull(new Float(3.1));
		assertEquals(l, new Long(3));
		// Casting truncates
		l = SakaiBLTIUtil.getLongNull(new Float(3.9));
		assertEquals(l, new Long(3));
		l = SakaiBLTIUtil.getLongNull(new Integer(3));
		assertEquals(l, new Long(3));
	}

	@Test
	public void testConvertDouble() {
		Double d = SakaiBLTIUtil.getDoubleNull(new Double(2.0));
		assertEquals(d, new Double(2.0));
		d = SakaiBLTIUtil.getDoubleNull(new Double(2.5));
		assertEquals(d, new Double(2.5));
		d = SakaiBLTIUtil.getDoubleNull(null);
		assertEquals(d, null);
		d = SakaiBLTIUtil.getDoubleNull("fred");
		assertEquals(d, null);
		d = SakaiBLTIUtil.getDoubleNull("null");
		assertEquals(d, null);
		d = SakaiBLTIUtil.getDoubleNull("NULL");
		assertEquals(d, null);
		d = SakaiBLTIUtil.getDoubleNull("");
		assertEquals(d, null);
		d = SakaiBLTIUtil.getDoubleNull("2.0");
		assertEquals(d, new Double(2.0));
		d = SakaiBLTIUtil.getDoubleNull("2.5");
		assertEquals(d, new Double(2.5));
		d = SakaiBLTIUtil.getDoubleNull("2");
		assertEquals(d, new Double(2.0));
		d = SakaiBLTIUtil.getDoubleNull(new Long(3));
		assertEquals(d, new Double(3.0));
		d = SakaiBLTIUtil.getDoubleNull(new Integer(3));
		assertEquals(d, new Double(3.0));
	}

	@Test
	public void testFindBestTool() {
		List<Map<String,Object>> tools = new ArrayList<Map<String,Object>>();
		Map<String,Object> tool = new HashMap<String,Object>();

		String [] toolUrls = {
			"https://www.py4e.com/",
			"https://www.py4e.com/mod/",
			"https://www.py4e.com/mod/gift/",
			"https://www.py4e.com/mod/gift/?quiz=123"
		};

		String siteId = "tsugi-site";
		String leastSpecific = toolUrls[0];
		String mostSpecific = toolUrls[3];
		String bestSite;
		String bestLaunch;

		Map<String,Object> bestTool = null;

		tools = new ArrayList<Map<String,Object>>();
		// Lets make some globals in least specific to most specific
		for(String s: toolUrls) {
			tool.put(LTIService.LTI_LAUNCH, s);
			tool.put(LTIService.LTI_SITE_ID, ""); // Global
			tools.add(tool);

			bestTool = SakaiBLTIUtil.findBestToolMatch(s, tools);
			bestLaunch = (String) bestTool.get(LTIService.LTI_LAUNCH);
			bestSite = (String) bestTool.get(LTIService.LTI_SITE_ID);
			assertEquals(s, bestLaunch);
			assertEquals("", bestSite);

			bestTool = SakaiBLTIUtil.findBestToolMatch(mostSpecific, tools);
			bestLaunch = (String) bestTool.get(LTIService.LTI_LAUNCH);
			bestSite = (String) bestTool.get(LTIService.LTI_SITE_ID);
			assertEquals(s, bestLaunch);
			assertEquals("", bestSite);
		}


		tools = new ArrayList<Map<String,Object>>();
		// Lets make some globals in least specific to most specific
		for(String s: toolUrls) {
			tool.put(LTIService.LTI_LAUNCH, s);
			tool.put(LTIService.LTI_SITE_ID, ""); // Global
			tools.add(tool);

			bestTool = SakaiBLTIUtil.findBestToolMatch(s, tools);
			bestLaunch = (String) bestTool.get(LTIService.LTI_LAUNCH);
			bestSite = (String) bestTool.get(LTIService.LTI_SITE_ID);
			assertEquals(s, bestLaunch);
			assertEquals("", bestSite);

			bestTool = SakaiBLTIUtil.findBestToolMatch(mostSpecific, tools);
			bestLaunch = (String) bestTool.get(LTIService.LTI_LAUNCH);
			bestSite = (String) bestTool.get(LTIService.LTI_SITE_ID);
			assertEquals(s, bestLaunch);
			assertEquals("", bestSite);
		}

		// Lets add a local low priority - see if it wins
		tool.put(LTIService.LTI_LAUNCH, leastSpecific);
		tool.put(LTIService.LTI_SITE_ID, siteId);
		tools.add(tool);

		bestTool = SakaiBLTIUtil.findBestToolMatch(mostSpecific, tools);
		bestLaunch = (String) bestTool.get(LTIService.LTI_LAUNCH);
		bestSite = (String) bestTool.get(LTIService.LTI_SITE_ID);
		assertEquals(leastSpecific, bestLaunch);
		assertEquals(siteId, bestSite);

		// Lets make locals and globals, and make sure we never get a global
		tools = new ArrayList<Map<String,Object>>();
		for(String s: toolUrls) {
			tool.put(LTIService.LTI_LAUNCH, s);
			tool.put(LTIService.LTI_SITE_ID, ""); // Global
			tools.add(tool);
			tool.put(LTIService.LTI_LAUNCH, s);
			tool.put(LTIService.LTI_SITE_ID, siteId); // Local
			tools.add(tool);

			bestTool = SakaiBLTIUtil.findBestToolMatch(s, tools);
			bestLaunch = (String) bestTool.get(LTIService.LTI_LAUNCH);
			bestSite = (String) bestTool.get(LTIService.LTI_SITE_ID);
			assertEquals(s, bestLaunch);
			assertEquals(siteId, bestSite);

			bestTool = SakaiBLTIUtil.findBestToolMatch(mostSpecific, tools);
			bestLaunch = (String) bestTool.get(LTIService.LTI_LAUNCH);
			bestSite = (String) bestTool.get(LTIService.LTI_SITE_ID);
			assertEquals(s, bestLaunch);
			assertEquals(siteId, bestSite);
		}

	}

	/* Quick story.  When reviewing PR#8884 - the collective wisdom was not to
	 * just scan for a question mark and chop.  MJ said use the URI builder.
	 * CS was worried that it would do weird things to the string like add or
	 * remove a :443 in an attempt to make the URL "better".
	 * So I wrote a unit test to defend against that eventuality and here it is.
	 */
	public String crudeButEffectiveStripOffQuery(String urlString)
	{
		if ( urlString == null ) return null;
        String retval = urlString;
        int pos = retval.indexOf('?');
        if ( pos > 1 ) {
            retval = retval.substring(0,pos);
        }
        return retval;
	}

	@Test
	public void testStripOffQuery() {
		String testUrls[] = {
			"http://localhost:8080",
			"http://localhost:8080/",
			"http://localhost:8080/zap",
			"http://localhost:8080/zap/",
			"http://localhost:8080/zap/bob.php?x=1234",
			"https://www.py4e.com",
			"https://www.py4e.com/",
			"https://www.py4e.com/zap/",
			"https://www.py4e.com/zap/bob.php?x=1234",
			"https://www.py4e.com:443/zap/bob.php?x=1234",
			"https://www.sakailms.org/"
		};

		for(String s: testUrls) {
			assertEquals(SakaiBLTIUtil.stripOffQuery(s), crudeButEffectiveStripOffQuery(s));
		}
	}

	@Test
	public void testTrackResourceLinkID() {
		Map<String, Object> oldContent = new TreeMap<String, Object> ();
		JSONObject old_json = BasicLTIUtil.parseJSONObject("");
		old_json.put(LTIService.LTI_ID_HISTORY,"content:1,content:2");
		oldContent.put(LTIService.LTI_SETTINGS, old_json.toString());
		oldContent.put(LTIService.LTI_ID, "4");

		String post = SakaiBLTIUtil.trackResourceLinkID(oldContent);
		assertEquals(post, "content:1,content:2,content:4");

		Map<String, Object> newContent = new TreeMap<String, Object> ();
		JSONObject new_json = BasicLTIUtil.parseJSONObject("");
		new_json.put(LTIService.LTI_ID_HISTORY,"content:2,content:3");
		newContent.put(LTIService.LTI_SETTINGS, new_json.toString());

		boolean retval = SakaiBLTIUtil.trackResourceLinkID(newContent, oldContent);
		assertTrue(retval);

		post = (String) newContent.get(LTIService.LTI_SETTINGS);
		JSONObject post_json = BasicLTIUtil.parseJSONObject(post);
		String post_history = (String) post_json.get(LTIService.LTI_ID_HISTORY);
		assertEquals(post_history, "content:1,content:2,content:3,content:4");

		// Verify no double add
		retval = SakaiBLTIUtil.trackResourceLinkID(newContent, oldContent);
		assertFalse(retval);

		// Have an empty settings in the newContent item (typical use case);
		newContent.remove(LTIService.LTI_SETTINGS);
		retval = SakaiBLTIUtil.trackResourceLinkID(newContent, oldContent);

		post = (String) newContent.get(LTIService.LTI_SETTINGS);
		post_json = BasicLTIUtil.parseJSONObject(post);
		post_history = (String) post_json.get(LTIService.LTI_ID_HISTORY);
		assertEquals(post_history, "content:1,content:2,content:4");
	}

	// TODO: For now make sure this does not blow up - later check the actual output :)
	@Test
	public void testConvertRoleMapPropToMap() {
		String roleMap = "sakairole1:ltirole1,sakairole2:ltirole2";
		Map retval = SakaiBLTIUtil.convertOutboundRoleMapPropToMap(roleMap);
		assertTrue(retval instanceof Map);
		assertTrue(retval.size() == 2);

        // * Using semicolon as the delimiter allows you to indicate more than one IMS role.
		roleMap = "sakairole4:ltirole4,ltirole5;sakairole6:ltirole6";
		retval = SakaiBLTIUtil.convertOutboundRoleMapPropToMap(roleMap);
		assertTrue(retval instanceof Map);
		assertTrue(retval.size() == 2);

		roleMap = "maintain:"+BasicLTIConstants.MEMBERSHIP_ROLE_CONTEXT_ADMIN +
                "," + BasicLTIConstants.MEMBERSHIP_ROLE_SYSTEM_ADMIN +
                "," + BasicLTIConstants.MEMBERSHIP_ROLE_INSTITUTION_ADMIN+ ";sakairole6:ltirole6";
		retval = SakaiBLTIUtil.convertOutboundRoleMapPropToMap(roleMap);
		assertTrue(retval instanceof Map);
		assertTrue(retval.size() == 2);

		// Semicolon at end
		roleMap = "maintain:"+BasicLTIConstants.MEMBERSHIP_ROLE_CONTEXT_ADMIN +
                "," + BasicLTIConstants.MEMBERSHIP_ROLE_SYSTEM_ADMIN +
                "," + BasicLTIConstants.MEMBERSHIP_ROLE_INSTITUTION_ADMIN+ ";sakairole6:ltirole6;";
		retval = SakaiBLTIUtil.convertOutboundRoleMapPropToMap(roleMap);
		assertTrue(retval instanceof Map);
		assertTrue(retval.size() == 2);

		// Semicolon at beginning
		roleMap = ";maintain:"+BasicLTIConstants.MEMBERSHIP_ROLE_CONTEXT_ADMIN +
                "," + BasicLTIConstants.MEMBERSHIP_ROLE_SYSTEM_ADMIN +
                "," + BasicLTIConstants.MEMBERSHIP_ROLE_INSTITUTION_ADMIN+ ";sakairole6:ltirole6";
		retval = SakaiBLTIUtil.convertOutboundRoleMapPropToMap(roleMap);
		assertTrue(retval instanceof Map);
		assertTrue(retval.size() == 2);

		// Many semicolon in the middle
		roleMap = "maintain:"+BasicLTIConstants.MEMBERSHIP_ROLE_CONTEXT_ADMIN +
                "," + BasicLTIConstants.MEMBERSHIP_ROLE_SYSTEM_ADMIN +
                "," + BasicLTIConstants.MEMBERSHIP_ROLE_INSTITUTION_ADMIN+ ";;;;sakairole6:ltirole6";
		retval = SakaiBLTIUtil.convertOutboundRoleMapPropToMap(roleMap);
		assertTrue(retval instanceof Map);
		assertTrue(retval.size() == 2);

		retval = SakaiBLTIUtil.convertOutboundRoleMapPropToMap(null);
		assertTrue(retval instanceof Map);
		assertTrue(retval.size() == 0);

		retval = SakaiBLTIUtil.convertOutboundRoleMapPropToMap("");
		assertTrue(retval instanceof Map);
		assertTrue(retval.size() == 0);

		retval = SakaiBLTIUtil.convertOutboundRoleMapPropToMap(" ");
		assertTrue(retval instanceof Map);
		assertTrue(retval.size() == 0);
	}

	@Test
	public void testDefaultRoleMap() {
		Map<String, String> roleMap = SakaiBLTIUtil.convertOutboundRoleMapPropToMap(SakaiBLTIUtil.LTI_OUTBOUND_ROLE_MAP_DEFAULT);

		assertTrue(roleMap instanceof Map);
		assertEquals(18, roleMap.size());
		assertTrue(roleMap.get("Yada") == null);
		assertEquals(roleMap.get("access"),      "Learner,http://purl.imsglobal.org/vocab/lis/v2/membership#Learner");
		assertEquals(roleMap.get("maintain"),    "Instructor,http://purl.imsglobal.org/vocab/lis/v2/membership#Instructor");
		assertEquals(roleMap.get("Student"),     "Learner,http://purl.imsglobal.org/vocab/lis/v2/membership#Learner");
		assertEquals(roleMap.get("Learner"),     "Learner,http://purl.imsglobal.org/vocab/lis/v2/membership#Learner");
		assertEquals(roleMap.get("Instructor"),  "Instructor,http://purl.imsglobal.org/vocab/lis/v2/membership#Instructor");
		assertEquals(roleMap.get("Guest"),       "Guest,http://purl.imsglobal.org/vocab/lis/v2/institution/person#Guest");
		assertEquals(roleMap.get("Observer"),    "Observer,http://purl.imsglobal.org/vocab/lis/v2/institution/person#Observer");
		// Blanks are really important below
		assertEquals(roleMap.get("Teaching Assistant"),    "TeachingAssistant,http://purl.imsglobal.org/vocab/lis/v2/membership#Instructor#TeachingAssistant");
	}

	@Test
	public void testInboundRoleMap() {
		Map<String, String> legacyMap = SakaiBLTIUtil.convertLegacyRoleMapPropToMap(SakaiBLTIUtil.LTI_LEGACY_ROLE_MAP_DEFAULT);
		assertTrue(legacyMap instanceof Map);
		assertEquals(legacyMap.size(), 10);
		assertTrue(legacyMap.get("Yada") == null);

		Map<String, List<String>> roleMap = SakaiBLTIUtil.convertInboundRoleMapPropToMap(SakaiBLTIUtil.LTI_INBOUND_ROLE_MAP_DEFAULT);
		assertTrue(roleMap instanceof Map);
		assertEquals(roleMap.size(), 23);
		assertTrue(roleMap.get("Yada") == null);

		List<String> roleList = roleMap.get("http://purl.imsglobal.org/vocab/lis/v2/membership#Learner");
		assertTrue(roleList.contains("Student"));
		assertTrue(roleList.contains("Learner"));
		assertTrue(roleList.contains("access"));

		roleList = roleMap.get(legacyMap.get("Learner"));
		assertTrue(roleList.contains("Student"));
		assertTrue(roleList.contains("Learner"));
		assertTrue(roleList.contains("access"));

		roleList = roleMap.get("http://purl.imsglobal.org/vocab/lis/v2/membership#Instructor");
		assertTrue(roleList.contains("Instructor"));
		assertTrue(roleList.contains("maintain"));

		roleList = roleMap.get(legacyMap.get("Instructor"));
		assertTrue(roleList.contains("Instructor"));
		assertTrue(roleList.contains("maintain"));

		roleList = roleMap.get("http://purl.imsglobal.org/vocab/lis/v2/membership#Instructor#TeachingAssistant");
		assertTrue(roleList.contains("Teaching Assistant")); // The blank is really important
		assertTrue(roleList.contains("Instructor"));
		assertTrue(roleList.indexOf("Instructor") > roleList.indexOf("Teaching Assistant"));
		assertTrue(roleList.contains("maintain"));
		assertTrue(roleList.indexOf("maintain") > roleList.indexOf("Instructor"));

	}

	// Local so as not to call ServerConfigurationService
	public static String mapOutboundRole(String sakaiRole, String toolOutboundMapStr)
	{
		Map<String, String> propLegacyMap = SakaiBLTIUtil.convertLegacyRoleMapPropToMap(
			"urn:lti:instrole:dude=http://purl.imsglobal.org/vocab/lis/v2/membership#Instructor#Dude"
		);
		Map<String, String> defaultLegacyMap = SakaiBLTIUtil.convertLegacyRoleMapPropToMap(SakaiBLTIUtil.LTI_LEGACY_ROLE_MAP_DEFAULT);

		Map<String, String> toolRoleMap = SakaiBLTIUtil.convertOutboundRoleMapPropToMap(toolOutboundMapStr);

		Map<String, String> propRoleMap = SakaiBLTIUtil.convertOutboundRoleMapPropToMap(
			"Dude:Dude,http://purl.imsglobal.org/vocab/lis/v2/institution/person#Abides;" +
			"Staff:Staff,Dude,http://purl.imsglobal.org/vocab/lis/v2/institution/person#Staff;"
		);
		Map<String, String> defaultRoleMap = SakaiBLTIUtil.convertOutboundRoleMapPropToMap(SakaiBLTIUtil.LTI_OUTBOUND_ROLE_MAP_DEFAULT);

		return SakaiBLTIUtil.mapOutboundRole(sakaiRole, toolRoleMap, propRoleMap, defaultRoleMap, propLegacyMap, defaultLegacyMap);
	}

	@Test
	public void testOutbound() {
		String toolProp = "ToolI:Instructor;ToolM:Instructor,Learner;ToolA:"+BasicLTIConstants.MEMBERSHIP_ROLE_INSTITUTION_ADMIN+";";

		String imsRole = mapOutboundRole("maintain", toolProp);
		assertEquals("Instructor,http://purl.imsglobal.org/vocab/lis/v2/membership#Instructor", imsRole);

		imsRole = mapOutboundRole("Instructor", toolProp);
		assertEquals("Instructor,http://purl.imsglobal.org/vocab/lis/v2/membership#Instructor", imsRole);

		imsRole = mapOutboundRole("Baby Yoda", toolProp);
		assertTrue(imsRole == null);

		imsRole = mapOutboundRole("TeachingAssistant", toolProp);
		assertTrue(imsRole == null);

		imsRole = mapOutboundRole("Teaching Assistant", toolProp);
		assertEquals("TeachingAssistant,http://purl.imsglobal.org/vocab/lis/v2/membership#Instructor#TeachingAssistant", imsRole);

		imsRole = mapOutboundRole("admin", toolProp);
		assertTrue(imsRole.contains("Instructor"));
		assertTrue(imsRole.contains("Administrator"));

		imsRole = mapOutboundRole("Guest", toolProp);
		assertEquals("Guest,http://purl.imsglobal.org/vocab/lis/v2/institution/person#Guest", imsRole);

		// Extra from properties
		imsRole = mapOutboundRole("Dude", toolProp);
		assertEquals("Dude,http://purl.imsglobal.org/vocab/lis/v2/institution/person#Abides", imsRole);

		imsRole = mapOutboundRole("Staff", toolProp);
		assertEquals("Staff,Dude,http://purl.imsglobal.org/vocab/lis/v2/institution/person#Staff", imsRole);

		// Tool maps to legacy Instructor - upconverted
		imsRole = mapOutboundRole("ToolI", toolProp);
		assertEquals("http://purl.imsglobal.org/vocab/lis/v2/membership#Instructor", imsRole);

		// Tool maps to legacy admin - upconverted
		imsRole = mapOutboundRole("ToolA", toolProp);
		assertEquals("http://purl.imsglobal.org/vocab/lis/v2/institution/person#Administrator", imsRole);

		// Tool maps to legacy admin - upconverted
		imsRole = mapOutboundRole("ToolM", toolProp);
		assertEquals("http://purl.imsglobal.org/vocab/lis/v2/membership#Instructor,http://purl.imsglobal.org/vocab/lis/v2/membership#Learner", imsRole);
	}

	// Local to avoid ServerConfiguration process
	public static String mapInboundRole(String incomingRoles, Set<String> siteRoles, String tenantInboundMapStr)
	{
		// Helps upgrade legacy roles like Instructor or urn:lti:sysrole:ims/lis/Administrator
		Map<String, String> propLegacyMap = SakaiBLTIUtil.convertLegacyRoleMapPropToMap(
			"urn:lti:instrole:dude=http://purl.imsglobal.org/vocab/lis/v2/membership#Instructor#Dude"
		);
		Map<String, String> defaultLegacyMap = SakaiBLTIUtil.convertLegacyRoleMapPropToMap(SakaiBLTIUtil.LTI_LEGACY_ROLE_MAP_DEFAULT);

		Map<String, List<String>> tenantInboundMap = SakaiBLTIUtil.convertInboundRoleMapPropToMap(tenantInboundMapStr);
		Map<String, List<String>> propInboundMap = null; // SakaiBLTIUtil.convertInboundRoleMapPropToMap( ServerConfigurationService.getString(SakaiBLTIUtil.LTI_INBOUND_ROLE_MAP));
		Map<String, List<String>> defaultInboundMap = SakaiBLTIUtil.convertInboundRoleMapPropToMap(SakaiBLTIUtil.LTI_INBOUND_ROLE_MAP_DEFAULT);

		return SakaiBLTIUtil.mapInboundRole(incomingRoles, siteRoles, tenantInboundMap, propInboundMap, defaultInboundMap, propLegacyMap, defaultLegacyMap);
	}

	@Test
	public void testInbound() {

		String sakaiRole = mapInboundRole("http://purl.imsglobal.org/vocab/lis/v2/membership#Instructor", projectRoles, null);
		assertEquals("maintain", sakaiRole);

		sakaiRole = mapInboundRole("http://purl.imsglobal.org/vocab/lis/v2/membership#Learner", projectRoles, null);
		assertEquals("access", sakaiRole);

		sakaiRole = mapInboundRole("urn:canvas:instructor,http://purl.imsglobal.org/vocab/lis/v2/membership#Instructor,urn:sakai:dude", projectRoles, null);
		assertEquals("maintain", sakaiRole);

		sakaiRole = mapInboundRole("http://purl.imsglobal.org/vocab/lis/v2/membership#Learner", projectRoles, null);
		assertEquals("access", sakaiRole);

		sakaiRole = mapInboundRole("http://purl.imsglobal.org/vocab/lis/v2/membership#Instructor", courseRoles, null);
		assertEquals("Instructor", sakaiRole);

		sakaiRole = mapInboundRole("http://purl.imsglobal.org/vocab/lis/v2/membership#Learner", courseRoles, null);
		assertEquals("Student", sakaiRole);

		sakaiRole = mapInboundRole("http://purl.imsglobal.org/vocab/lis/v2/membership#Instructor#TeachingAssistant", courseRoles, null);
		assertEquals("Teaching Assistant", sakaiRole);

		sakaiRole = mapInboundRole("urn:canvas:instructor,http://purl.imsglobal.org/vocab/lis/v2/membership#Instructor,urn:sakai:dude", courseRoles, null);
		assertEquals("Instructor", sakaiRole);

		sakaiRole = mapInboundRole("http://purl.imsglobal.org/vocab/lis/v2/membership#Learner", courseRoles, null);
		assertEquals("Student", sakaiRole);

		sakaiRole = mapInboundRole("http://purl.imsglobal.org/vocab/lis/v2/membership#Instructor", ltiRoles, null);
		assertEquals("Instructor", sakaiRole);

		sakaiRole = mapInboundRole("http://purl.imsglobal.org/vocab/lis/v2/membership#Learner", ltiRoles, null);
		assertEquals("Learner", sakaiRole);

		sakaiRole = mapInboundRole("urn:canvas:instructor,http://purl.imsglobal.org/vocab/lis/v2/membership#Instructor,urn:sakai:dude", ltiRoles, null);
		assertEquals("Instructor", sakaiRole);

		sakaiRole = mapInboundRole("http://purl.imsglobal.org/vocab/lis/v2/membership#Learner", ltiRoles, null);
		assertEquals("Learner", sakaiRole);

		sakaiRole = mapInboundRole("http://purl.imsglobal.org/vocab/lis/v2/membership#Learner", ltiRoles, null);
		assertEquals("Learner", sakaiRole);

		// Context roles from https://www.imsglobal.org/spec/lti/v1p3/#role-vocabularies
		for (String s : "ContentDeveloper,Instructor,Learner,Mentor,Manager,Member,Officer".split(",") ) {
			sakaiRole = mapInboundRole("http://purl.imsglobal.org/vocab/lis/v2/membership#" + s, ltiRoles, null);
			assertEquals(s, sakaiRole);
		}

		// Institution but not context roles from https://www.imsglobal.org/spec/lti/v1p3/#role-vocabularies
		// We don't do person/Student as it maps to Learner
		for (String s : "Faculty,Guest,None,Other,Staff,Alumni,Observer,ProspectiveStudent".split(",") ) {
			sakaiRole = mapInboundRole("http://purl.imsglobal.org/vocab/lis/v2/institution/person#" + s, ltiRoles, null);
			assertEquals(s, sakaiRole);
		}
	}

	@Test
	public void testInception() {
		String imsRole;
		String sakaiRole;

		for (String roundTrip : "Instructor,Learner,Guest,Teaching Assistant,Mentor,Alumni,ProspectiveStudent".split(",") ) {
			imsRole = mapOutboundRole(roundTrip, null);
			sakaiRole = mapInboundRole(imsRole, ltiRoles, null);
			assertEquals(roundTrip, sakaiRole);
			imsRole = mapOutboundRole(sakaiRole, null);
			sakaiRole = mapInboundRole(imsRole, ltiRoles, null);
			assertEquals(roundTrip, sakaiRole);
		}

		for (String roundTrip : "Instructor,Student,Teaching Assistant".split(",") ) {
			imsRole = mapOutboundRole(roundTrip, null);
			sakaiRole = mapInboundRole(imsRole, courseRoles, null);
			assertEquals(roundTrip, sakaiRole);
			imsRole = mapOutboundRole(sakaiRole, null);
			sakaiRole = mapInboundRole(imsRole, courseRoles, null);
			assertEquals(roundTrip, sakaiRole);
		}

		for (String roundTrip : "access,maintain".split(",") ) {
			imsRole = mapOutboundRole(roundTrip, null);
			sakaiRole = mapInboundRole(imsRole, projectRoles, null);
			assertEquals(roundTrip, sakaiRole);
			imsRole = mapOutboundRole(sakaiRole, null);
			sakaiRole = mapInboundRole(imsRole, projectRoles, null);
			assertEquals(roundTrip, sakaiRole);
		}
	}

	public String compileJavaScript(String extraJS) {
		long count = extraJS.chars().filter(ch -> ch == '{').count();
		long count2 = extraJS.codePoints().filter(ch -> ch == '}').count();
		if ( count != count2 ) {
			System.out.println(extraJS);
			return "{} mismatch";
		}
		count = extraJS.chars().filter(ch -> ch == '(').count();
		count2 = extraJS.codePoints().filter(ch -> ch == '(').count();
		if ( count != count2 ) {
			System.out.println(extraJS);
			return "() mismatch";
		}
		count = extraJS.chars().filter(ch -> ch == '"').count();
		assertEquals(count % 2, 0);
		if ( count % 2 != 0 ) {
			System.out.println(extraJS);
			return " \" mismatch";
		}
		count = extraJS.chars().filter(ch -> ch == '\'').count();
		if ( count % 2 != 0 ) {
			System.out.println(extraJS);
			return " ' mismatch";
		}
		return "success";
	}

	@Test
	public void testFormPost() {
		boolean autosubmit = true;
		String submit_form_id = "42";
		String extraJS = SakaiBLTIUtil.getLaunchJavaScript(submit_form_id, autosubmit);
		assertTrue(extraJS.contains("document.getElementById"));
		assertEquals(compileJavaScript(extraJS), "success");

		autosubmit = false;
		extraJS = SakaiBLTIUtil.getLaunchJavaScript(submit_form_id, autosubmit);
		assertFalse(extraJS.contains("document.getElementById"));
		assertEquals(compileJavaScript(extraJS), "success");

		String launch_url = "https://www.tsugicloud.org/lti/store";
		String jws = "IAMJWS";
		String ljs = "{ \"key\": \"Value\"} ";
		String state = "42";
		String launch_error = "Dude abides";

		boolean dodebug = false;
		String form = SakaiBLTIUtil.getJwsHTMLForm(launch_url, "id_token", jws, ljs, state, launch_error, dodebug);
		assertEquals(compileJavaScript(form), "success");
		assertTrue(form.contains("document.getElementById"));

		dodebug = true;
		form = SakaiBLTIUtil.getJwsHTMLForm(launch_url, "id_token", jws, ljs, state, launch_error, dodebug);
		assertEquals(compileJavaScript(form), "success");
		assertFalse(form.contains("document.getElementById"));


	}
}


