package com.cloudamqp.rabbitteste;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ConsumerCancelledException;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.AMQP.Queue.DeclareOk;
import com.rabbitmq.client.ShutdownSignalException;

import org.json.JSONException;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;

import android.app.ActionBar;
import android.app.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONObject;
import org.json.simple.parser.ParseException;

public class Chat extends Activity {

    private String user;
    private String dest;
    private Date data;

    Thread subscribeThread;
    Thread publishThread;

    private BlockingDeque<String> queue = new LinkedBlockingDeque<String>();

    ConnectionFactory factory = new ConnectionFactory();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat);
        Intent it = getIntent();
        user = it.getStringExtra("nomeUser");
        dest = it.getStringExtra("nomeDest");

        final Handler incomingMessageHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                String message = msg.getData().getString("msg");
                String nome = msg.getData().getString("nome");
                String data = msg.getData().getString("data");
                TextView tv = (TextView) findViewById(R.id.textView);
                tv.append("(" + data + ") " + nome + " disse: " + message + '\n');
                Log.d("HANDLER", message);
            }
        };
        setupConnectionFactory();
        publishToAMQP(incomingMessageHandler);
        setupPubButton();
        subscribe(incomingMessageHandler);

        // Get a support ActionBar corresponding to this toolbar
        getActionBar().setDisplayHomeAsUpEnabled(true);

    }

    void setupPubButton() {
        Button button = (Button) findViewById(R.id.buttonEnviar);
        button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                EditText et = (EditText) findViewById(R.id.text);
                publishMessage(et.getText().toString());
                et.setText("");
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        publishThread.interrupt();
        subscribeThread.interrupt();
    }

    void publishMessage(String message) {
        //Adds a message to internal blocking queue
        try {
            Log.d("","[q] " + message);
            queue.putLast(message);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void setupConnectionFactory() {
        factory.setHost("34.239.228.190 ");
        factory.setUsername("teste");
        factory.setPassword("teste");
        // Coloca aqui a url la do cloud amqp
        //String uri = "";
        //try {
            factory.setAutomaticRecoveryEnabled(false);
            //factory.setUri(uri);
        //} catch (KeyManagementException | NoSuchAlgorithmException | URISyntaxException e1) {
        //    e1.printStackTrace();
        //}
    }

    void subscribe(final Handler handler)
    {
        subscribeThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Connection connection = null;
                Channel channel = null;
                try {
                    connection = factory.newConnection();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (TimeoutException e) {
                    e.printStackTrace();
                }
                try {
                    channel = connection.createChannel();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                String nome;
                String data;
                String mensagem;

                QueueingConsumer consumer = new QueueingConsumer(channel);

                //Cria um Objeto JSON
                JSONObject jsonObject;
                JSONObject jsonMessage;

                while(true) {
                    try {
                        channel.basicConsume(user, true, consumer);

                        QueueingConsumer.Delivery delivery = null;
                        try {
                            delivery = consumer.nextDelivery();
                        } catch (InterruptedException ex) {
                            Log.d("", ex.getClass().getName());
                        } catch (ShutdownSignalException ex) {
                            Log.d("", ex.getClass().getName());
                        } catch (ConsumerCancelledException ex) {
                            Log.d("", ex.getClass().getName());
                        }

                        String message = new String(delivery.getBody());
                        jsonMessage = new JSONObject(message);

                        //Salva nas variaveis os dados retirados do arquivo
                        jsonObject = (JSONObject) jsonMessage.get("message");
                        nome = (String) jsonObject.get("sender");
                        data = (String) jsonObject.get("date");
                        mensagem = (String) jsonObject.get("content");

                        Log.d("","[r] " + mensagem);
                        Message msg = handler.obtainMessage();
                        Bundle bundle = new Bundle();
                        bundle.putString("msg", mensagem);
                        bundle.putString("nome", nome);
                        bundle.putString("data", data);
                        msg.setData(bundle);
                        handler.sendMessage(msg);
                    } catch (Exception e1) {
                        Log.d("", "Connection broken: " + e1.getClass().getName());
                        try {
                            Thread.sleep(5000); //sleep and then try again
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                }
            }
        });
        subscribeThread.start();
    }

    public void publishToAMQP(final Handler handler)
    {
        publishThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Connection connection = null;
                Channel channel = null;
                try {
                    connection = factory.newConnection();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (TimeoutException e) {
                    e.printStackTrace();
                }
                try {
                    channel = connection.createChannel();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    channel.queueDeclare(user, false, false, false, null);
                    channel.queueDeclare(dest, false, false, false, null);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                //Cria um Objeto JSON
                JSONObject jsonObject = new JSONObject();
                JSONObject jsonMessage = new JSONObject();

                DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");

                while(true) {
                    try {
                        channel.confirmSelect();

                        while (true) {
                            String message = queue.takeFirst();
                            //Captura a data e a hora que a mensagem foi enviada
                            data = new Date();

                            //Armazena dados em um Objeto JSON
                            try {
                                jsonObject.put("sender", user);
                                jsonObject.put("date", dateFormat.format(data).substring(0, 10) + " as " + dateFormat.format(data).substring(11));
                                jsonObject.put("content", message);
                                jsonMessage.put("message", jsonObject);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            Message msg = handler.obtainMessage();
                            Bundle bundle = new Bundle();
                            bundle.putString("msg", message);
                            bundle.putString("nome", "eu");
                            bundle.putString("data", dateFormat.format(data).substring(0, 10) + " as " + dateFormat.format(data).substring(11));
                            msg.setData(bundle);
                            handler.sendMessage(msg);

                            try{
                                channel.basicPublish("", dest, null, jsonMessage.toString().getBytes());
                                Log.d("", "[s] " + message);
                                channel.waitForConfirmsOrDie();
                            } catch (Exception e){
                                Log.d("","[f] " + message);
                                queue.putFirst(message);
                                throw e;
                            }
                        }
                    } catch (InterruptedException e) {
                        break;
                    } catch (Exception e) {
                        Log.d("", "Connection broken: " + e.getClass().getName());
                        try {
                            Thread.sleep(5000); //sleep and then try again
                        } catch (InterruptedException e1) {
                            break;
                        }
                    }
                }
            }
        });
        publishThread.start();
    }
}