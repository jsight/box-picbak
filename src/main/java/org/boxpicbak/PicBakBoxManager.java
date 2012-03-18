package org.boxpicbak;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import javax.swing.JOptionPane;
import javax.swing.tree.DefaultMutableTreeNode;

import org.boxpicbak.PicBakBoxNode.eNodeType;

import cn.com.believer.songyuanframework.openapi.storage.box.BoxExternalAPI;
import cn.com.believer.songyuanframework.openapi.storage.box.constant.BoxConstant;
import cn.com.believer.songyuanframework.openapi.storage.box.factories.BoxRequestFactory;
import cn.com.believer.songyuanframework.openapi.storage.box.functions.CreateFolderRequest;
import cn.com.believer.songyuanframework.openapi.storage.box.functions.CreateFolderResponse;
import cn.com.believer.songyuanframework.openapi.storage.box.functions.GetAccountTreeRequest;
import cn.com.believer.songyuanframework.openapi.storage.box.functions.GetAccountTreeResponse;
import cn.com.believer.songyuanframework.openapi.storage.box.functions.GetAuthTokenRequest;
import cn.com.believer.songyuanframework.openapi.storage.box.functions.GetAuthTokenResponse;
import cn.com.believer.songyuanframework.openapi.storage.box.functions.GetFileInfoRequest;
import cn.com.believer.songyuanframework.openapi.storage.box.functions.GetFileInfoResponse;
import cn.com.believer.songyuanframework.openapi.storage.box.functions.GetTicketRequest;
import cn.com.believer.songyuanframework.openapi.storage.box.functions.GetTicketResponse;
import cn.com.believer.songyuanframework.openapi.storage.box.functions.UploadRequest;
import cn.com.believer.songyuanframework.openapi.storage.box.functions.UploadResponse;
import cn.com.believer.songyuanframework.openapi.storage.box.impl.simple.SimpleBoxImpl;
import cn.com.believer.songyuanframework.openapi.storage.box.objects.BoxAbstractFile;
import cn.com.believer.songyuanframework.openapi.storage.box.objects.BoxException;
import cn.com.believer.songyuanframework.openapi.storage.box.objects.UploadResult;

public class PicBakBoxManager {
    private String apiKey;
    private String ticket;
    private String token;
    
    BoxExternalAPI box;
    
    public void connect() throws BoxException, IOException, InterruptedException {
        Config cfg = Config.getInstance();
        box = new SimpleBoxImpl();

        apiKey = cfg.getApiKey();
        ticket = cfg.getTicket();
        token = cfg.getToken();
        
        if (ticket == null || token == null) {
            GetTicketRequest getTicketRequest = BoxRequestFactory.createGetTicketRequest(apiKey);
            GetTicketResponse getTicketResponse = box.getTicket(getTicketRequest);
            if (!BoxConstant.STATUS_GET_TICKET_OK.equals(getTicketResponse.getStatus())) {
                throw new RuntimeException("Failed to get ticket: " + getTicketResponse.getStatus());
            }
    
            ticket = getTicketResponse.getTicket();
            JOptionPane.showMessageDialog(null, "A browser will now open... login, then return and click Ok", "Box.net Login", JOptionPane.OK_OPTION);
            String authUriStr = "http://www.box.net/api/1.0/auth/" + ticket;
            URI authUri;
            try {
                authUri = new URL(authUriStr).toURI();
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
            Desktop.getDesktop().browse(authUri);
            
            JOptionPane.showMessageDialog(null, "Press Ok once authenticated", "Press Ok once authenticated", JOptionPane.OK_OPTION);
            
            GetAuthTokenRequest getAuthTokenRequest = BoxRequestFactory.createGetAuthTokenRequest(apiKey, ticket);
            GetAuthTokenResponse getAuthTokenResponse = box.getAuthToken(getAuthTokenRequest);
            token = getAuthTokenResponse.getAuthToken();
        }
    }
    
    public PicBakBoxNode getDirectory(String path, boolean fullTree) throws BoxException, IOException {
        String nd = null;
        PicBakBoxNode resultNode;
        if (path.equals("/")) {
            nd = "0";
            resultNode = getDirectoryByID(nd, fullTree);
        } else {
            PicBakBoxNode curBoxNode = getDirectoryByID("0", false);
            //System.out.println("Parent nd: " + curBoxNode);
            StringTokenizer stk = new StringTokenizer(path, "/");
            while (stk.hasMoreTokens()) {
                String nextPathToken = stk.nextToken();
                //System.out.println("Token: " + nextPathToken);
                boolean foundChild = false;
                for (int i = 0; i < curBoxNode.getChildren().size(); i++) {
                    PicBakBoxNode curBoxChild = curBoxNode.getChildren().get(i);
                    //System.out.println("\t" + curBoxChild.getNodeName());
                    if (curBoxChild.getNodeName().equals(nextPathToken)) {
                        foundChild = true;
                        curBoxNode = getDirectoryByID(curBoxChild.getNodeID(), false);
                        break;
                    }
                }
                if (!foundChild) {
                    curBoxNode = null;
                    break;
                }
            }
            if (curBoxNode != null) {
                resultNode = getDirectoryByID(curBoxNode.getNodeID(), fullTree);
            } else {
                resultNode = null;
            }
        }
        return resultNode;
    }
    
    /** 
     * Loads the SHA1 hash information
     * 
     * @param file
     * @return
     */
    public PicBakBoxNode getFileInfo(PicBakBoxNode file) throws BoxException, IOException {
        if (file.getNodeType() == eNodeType.folder) {
            throw new IllegalArgumentException("Folder type not supported!");
        } else {
            GetFileInfoRequest getFileInfoRequest = BoxRequestFactory.createGetFileInfoRequest(apiKey, token, file.getNodeID());
            GetFileInfoResponse getFileInfoResponse = box.getFileInfo(getFileInfoRequest);
            String sha1 = getFileInfoResponse.getFile().getSha1();
            file.setNodeSHA1(sha1);
            return file;
        }
    }
    
    private PicBakBoxNode getDirectoryByID(String nodeID, boolean fullTree) throws BoxException, IOException {
        String[] params;
        if (fullTree) {
            params = new String[]{ "nozip" };
        } else {
            params = new String[]{ "nozip", "onelevel" };
        }
        GetAccountTreeRequest getAccountTreeRequest = BoxRequestFactory.createGetAccountTreeRequest(apiKey, token, nodeID, params);
        GetAccountTreeResponse accountTreeResponse = box.getAccountTree(getAccountTreeRequest);
        DefaultMutableTreeNode node = accountTreeResponse.getTree();
        return createNode(node);
    }
    
    public PicBakBoxNode getFullDirectoryTree() throws BoxException, IOException {
        return getDirectory("/", true);
    }
    
    public PicBakBoxNode upload(File localFile, PicBakBoxNode parent) throws BoxException, IOException {
        Map<String, File> uploadParams = new HashMap<String, File>();
        uploadParams.put(localFile.getName(), localFile);
        UploadRequest uploadRequest = BoxRequestFactory.createUploadRequest(token, true, parent.getNodeID(), uploadParams);
        UploadResponse uploadResponse = box.upload(uploadRequest);
        PicBakBoxNode node = new PicBakBoxNode();
        UploadResult uploadResult = (UploadResult)uploadResponse.getUploadResultList().get(0);
        node.setNodeID(uploadResult.getFile().getFileId());
        node.setNodeName(localFile.getName());
        return node;
    }
    
    public PicBakBoxNode createFolder(PicBakBoxNode parent, String name) throws BoxException, IOException {
        CreateFolderRequest createFolderRequest = BoxRequestFactory.createCreateFolderRequest(apiKey, token, parent.getNodeID(), name, false);
        CreateFolderResponse createFolderResponse = box.createFolder(createFolderRequest);
        PicBakBoxNode node = new PicBakBoxNode();
        String newFolderID = createFolderResponse.getFolder().getFolderId();
        node.setNodeID(newFolderID);
        node.setNodeName(name);
        node.setNodeType(eNodeType.folder);
        return node;
    }
    
    private PicBakBoxNode createNode(DefaultMutableTreeNode boxNode) {
        PicBakBoxNode nd = new PicBakBoxNode();
        Object userObj = boxNode.getUserObject();
        BoxAbstractFile boxFile = (BoxAbstractFile)userObj;
        nd.setNodeType(boxFile.isFolder() ? PicBakBoxNode.eNodeType.folder : PicBakBoxNode.eNodeType.file);
        nd.setNodeID(boxFile.getId());
        nd.setNodeName(boxFile.getName());
        
        for (int i = 0; i < boxNode.getChildCount(); i++) {
            DefaultMutableTreeNode boxNodeChild = (DefaultMutableTreeNode)boxNode.getChildAt(i);
            PicBakBoxNode childNd = createNode(boxNodeChild);
            nd.getChildren().add(childNd);
        }
        return nd;
    }
}
