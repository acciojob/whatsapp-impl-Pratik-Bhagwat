package com.driver;

import java.util.*;

import org.springframework.stereotype.Repository;

@Repository
public class WhatsappRepository {

    //Assume that each user belongs to at most one group
    //You can use the below mentioned hashmaps or delete these and create your own.
    private HashMap<Group, List<User>> groupUserMap;
    private HashMap<Group, List<Message>> groupMessageMap;
    private HashMap<Message, User> senderMap;
    private HashMap<Group, User> adminMap;
    private HashMap<String,String> userMobile;
    private int customGroupCount;
    private int messageId;

    public WhatsappRepository(){
        this.groupMessageMap = new HashMap<Group, List<Message>>();
        this.groupUserMap = new HashMap<Group, List<User>>();
        this.senderMap = new HashMap<Message, User>();
        this.adminMap = new HashMap<Group, User>();
        this.userMobile = new HashMap<>();
        this.customGroupCount = 0;
        this.messageId = 0;
    }

    public String createUser (String name, String mobile) throws Exception {
        if(!userMobile.containsKey(mobile)) {
            userMobile.put(mobile,name);
            return "SUCCESS";
        }
        else throw new Exception("User already exists");
    }

    public Group createGroup(List<User> users) {
        int userCnt = users.size();
        String lastUser = users.get(1).getName();
        String admin = users.get(0).getName();

        if(userCnt == 2) {
            Group group = new Group(lastUser,userCnt);
            return group;
        }
        else if (userCnt > 2) {
            Group group = new Group("Group "+this.customGroupCount,userCnt);
            this.customGroupCount++;
            adminMap.put(group,users.get(0));
            groupUserMap.put(group,users);
            return group;
        }
        return null;
    }

    public int createMessage(String content) {
        String[] arr = content.split(" ");
        messageId = arr.length;
        return messageId;
    }

    public int sendMessage(Message message, User sender, Group group) throws Exception {
        //Throw "Group does not exist" if the mentioned group does not exist
        if(!adminMap.containsKey(group)) {
            throw new Exception("Group does not exist");
        }
        //Throw "You are not allowed to send message" if the sender is not a member of the group
        if(!groupUserMap.containsKey(sender)) {
            throw new Exception("You are not allowed to send message");
        }
        //If the message is sent successfully, return the final number of messages in that group.
        int finalMessageNumber = 0;

        if(groupMessageMap.containsKey(group)) {
            List<Message> oldList = groupMessageMap.get(group);
            oldList.add(message);
            senderMap.put(message,sender);
            finalMessageNumber++;
            groupMessageMap.put(group,oldList);
        }
        else {
            List<Message> messageList = new ArrayList<>();
            messageList.add(message);
            senderMap.put(message,sender);
            finalMessageNumber++;
            groupMessageMap.put(group,messageList);
        }
        return finalMessageNumber;
    }

    public String changeAdmin(User approver, User user, Group group) throws Exception {
        //Throw "Group does not exist" if the mentioned group does not exist
        if(!adminMap.containsKey(group)) {
            throw new Exception("Group does not exist");
        }
        //Throw "Approver does not have rights" if the approver is not the current admin of the group
        if(adminMap.get(group) != approver) {
            throw new Exception("Approver does not have rights");
        }
        //Throw "User is not a participant" if the user is not a part of the group
        if(groupUserMap.containsKey(group)) {
            List<User> newUsers = groupUserMap.get(group);
            if(!newUsers.contains(user)) {
                throw new Exception("User is not a participant");
            }
        }
        //Change the admin of the group to "user" and return "SUCCESS". Note that at one time there is only one admin and the admin rights are transferred from approver to user.
        if(adminMap.containsKey(group)) {
            adminMap.put(group,user);
        }
        return "SUCCESS";
    }

    public int removeUser(User user) throws Exception {
        List<User> userList = groupUserMap.get(user);
        int numberOfMessages = 0;

        //This is a bonus problem and does not contains any marks
        //A user belongs to exactly one group
        //If user is not found in any group, throw "User not found" exception
        if(!userList.contains(user)) {
            throw new Exception("User not found");
        }
        //If user is found in a group and it is the admin, throw "Cannot remove admin" exception
        if(userList.contains(user) && userList.get(0) == user) {
            throw new Exception("Cannot remove admin");
        }

        //If user is not the admin, remove the user from the group, remove all its messages from all the databases, and update relevant attributes accordingly.
        if(userList.contains(user) && userList.get(0) != user) {
            Message message = null;
            for(Message m: senderMap.keySet()) {
                User u = senderMap.get(m);
                if(u.getName().equals(user.getName())) {
                    message = m;
                    senderMap.remove(m);
                }
            }
            for(Group g: groupMessageMap.keySet()) {
                List<Message> messageList = groupMessageMap.get(g);
                if(messageList.contains(message)) {
                    messageList.remove(message);
                }
                numberOfMessages = messageList.size();
            }
           userList.remove(user);
        }
        //If user is removed successfully, return (the updated number of users in the group + the updated number of messages in group + the updated number of overall messages)
        int totalMessages = 0;
        for(List<Message> m : groupMessageMap.values()) {
           totalMessages += m.size();
        }
        return userList.size() + numberOfMessages + totalMessages;
    }

    public String findMessage(Date start, Date end, int k) throws Exception {
        //This is a bonus problem and does not contains any marks
        // Find the Kth latest message between start and end (excluding start and end)
        // If the number of messages between given time is less than K, throw "K is greater than the number of messages" exception
        List<Message> messages = new ArrayList<>();
        for(Group group: groupMessageMap.keySet()){
            messages.addAll(groupMessageMap.get(group));
        }
        List<Message> filteredMessages = new ArrayList<>();
        for(Message message: messages){
            if(message.getTimestamp().after(start) && message.getTimestamp().before(end)){
                filteredMessages.add(message);
            }
        }
        if(filteredMessages.size() < k){
            throw new Exception("K is greater than the number of messages");
        }
        Collections.sort(filteredMessages, new Comparator<Message>(){
            public int compare(Message m1, Message m2){
                return m2.getTimestamp().compareTo(m1.getTimestamp());
            }
        });
        return filteredMessages.get(k-1).getContent();
    }
}
