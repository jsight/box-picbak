package org.boxpicbak;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.List;

import cn.com.believer.songyuanframework.openapi.storage.box.objects.BoxException;


public class Main {
    public static void main(String[] argv) throws Exception {
        String localDir = argv[0];
        String remoteDir = argv[1];
        
        PicBakBoxManager mgr = new PicBakBoxManager();
        mgr.connect();
        
        PicBakBoxNode folder = mgr.getDirectory(remoteDir, true);
        System.out.println("Folder: " + folder);
        
        uploadDir(mgr, localDir, folder);
    }
    
    private static void uploadDir(PicBakBoxManager mgr, String localDirStr, PicBakBoxNode remoteParent) throws BoxException, IOException {
        File localParentDir = new File(localDirStr);
        File[] localChildren = localParentDir.listFiles();
        for (File localChild : localChildren) {
            PicBakBoxNode remoteChild = findRemoteChild(remoteParent, localChild.getName());
            if (localChild.isDirectory()) {
                if (remoteChild == null) {
                    remoteChild = mgr.createFolder(remoteParent, localChild.getName());
                }
                uploadDir(mgr, localChild.getAbsolutePath(), remoteChild);
            } else {
                boolean shaMatch = false;
                if (remoteChild != null) {
                    String localSha1 = calculateSHA1(localChild);
                    mgr.getFileInfo(remoteChild);
                    String remoteSha1 = remoteChild.getNodeSHA1();
                    shaMatch = localSha1.equals(remoteSha1);
                }
                if (!shaMatch) {
                    System.out.println("Uploading: " + localChild.getAbsolutePath());
                    mgr.upload(localChild, remoteParent);
                    System.out.println("Uploaded: " + localChild.getAbsolutePath());
                } else {
                    System.out.println("Skipping already uplaoded: " + localChild.getAbsolutePath());
                }
            }
        }
    }
    
    private static PicBakBoxNode findRemoteChild(PicBakBoxNode node, String name) {
        List<PicBakBoxNode> remoteChildNodes = node.getChildren();
        PicBakBoxNode result = null;
        for (PicBakBoxNode remoteChildNode : remoteChildNodes) {
            if (remoteChildNode.getNodeName().equals(name)) {
                result = remoteChildNode;
                break;
            }
        }
        return result;
    }
    
    private static String calculateSHA1(File file) throws IOException {
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA1");
            
            FileInputStream     fis = new FileInputStream(file);
            BufferedInputStream bis = new BufferedInputStream(fis);
            DigestInputStream   dis = new DigestInputStream(bis, sha1);
    
            // read the file and update the hash calculation
            while (dis.read() != -1);
    
            // get the hash value as byte array
            byte[] hash = sha1.digest();
    
            return byteArray2Hex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
    
    private static String byteArray2Hex(byte[] hash) {
        Formatter formatter = new Formatter();
        for (byte b : hash) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }
}
