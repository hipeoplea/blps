import { Client } from "@stomp/stompjs";
import WebSocket from "ws";

const chatId = "3d150aea-003e-46fa-a6eb-4d86414882f7";
const client = new Client({
    webSocketFactory: () => new WebSocket("ws://localhost:1488/ws"),
    reconnectDelay: 0,
    debug: console.log
});

client.onConnect = () => {
    client.subscribe(`/topic/chats/${chatId}/messages`, (msg) => {
        console.log("MESSAGE:", msg.body);
    });
    console.log("Subscribed");
};

client.activate();