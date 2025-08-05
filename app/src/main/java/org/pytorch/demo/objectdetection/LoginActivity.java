package org.pytorch.demo.objectdetection;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class LoginActivity extends AppCompatActivity {

    EditText et_email, et_password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        et_email = (EditText) findViewById(R.id.et_email);
        et_password = (EditText) findViewById(R.id.et_pass);

        accessMain();
    }

    private void accessMain() {
        Intent intent = new Intent(this, MainActivity.class);
        String matcher="vision";

        SharedPreferences data_user = getSharedPreferences("User_Pass", this.MODE_PRIVATE);
        SharedPreferences.Editor editor = data_user.edit();

        String user = et_email.getText().toString(), pass = et_password.getText().toString();

        editor.putString("Email", "null");
        editor.putString("Pass", "null");
        editor.commit();

        /**onClick para hacer el login a MainActivity*/
        Button login = findViewById(R.id.b_login);
        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String user = et_email.getText().toString();

                if (datosinsertados() == false) {
                    if (user.contains("visionanalytics.ai")) {
                        Toast.makeText(LoginActivity.this, "Bienvenid@ "+ user, Toast.LENGTH_LONG).show();
                        startActivity(intent);
                        LoginActivity.this.finish();
                    } else {
                        Toast.makeText(LoginActivity.this, "No se encuentra usuario, debe contener @visionanalytics.ai", Toast.LENGTH_SHORT).show();
                    }
                    limpiarCampos();
                }
            }
        });

        /**onClick para hacer el registro del usuario
        Button signup = findViewById(R.id.b_signup);
        signup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (datosinsertados() == true) {
                    if(user.matches("[a-zA-Z]+\\.[a-zA-Z]+\\@visionanalytics\\.ai")) {
                        editor.putString("User", user);
                        editor.putString("Pass", pass);
                        editor.commit();
                        Toast.makeText(LoginActivity.this, "Usuario creado con éxito", Toast.LENGTH_SHORT).show();
                    }
                    limpiarCampos();
                }
            }
        });*/

    }

    private boolean datosinsertados() {
        boolean error = false;
        String user = et_email.getText().toString(), pass = et_password.getText().toString();

        if (user.isEmpty() && pass.isEmpty()) {
            Toast.makeText(this, "Rellene todos los campos, por favor", Toast.LENGTH_SHORT).show();
            error = true;
        }
        return error;
    }

    private void limpiarCampos(){
        et_email.setText("");
        et_password.setText("");
    }
}
