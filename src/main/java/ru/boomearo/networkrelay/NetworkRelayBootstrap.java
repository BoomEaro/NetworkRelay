package ru.boomearo.networkrelay;

import ru.boomearo.networkrelay.app.NetworkRelayApp;

public class NetworkRelayBootstrap {

    public static void main(String[] args)  {
        NetworkRelayApp networkRelayApp = new NetworkRelayApp();
        networkRelayApp.load();

        Runtime.getRuntime().addShutdownHook(new Thread(networkRelayApp::unload));
    }

}