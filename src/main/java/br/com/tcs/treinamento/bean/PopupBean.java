package br.com.tcs.treinamento.bean;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import java.io.Serializable;

@ManagedBean(name = "popupBean")
@ViewScoped
public class PopupBean implements Serializable {

    private String textoDigitado;  // Nome digitado
    private String mensagemBoasVindas;

    public void salvar() {
        if (textoDigitado != null && !textoDigitado.trim().isEmpty()) {
            mensagemBoasVindas = "Bem-vindo, " + textoDigitado + "!";
        } else {
            mensagemBoasVindas = "Por favor, informe seu nome.";
        }
    }

    public String getTextoDigitado() {
        return textoDigitado;
    }

    public void setTextoDigitado(String textoDigitado) {
        this.textoDigitado = textoDigitado;
    }

    public String getMensagemBoasVindas() {
        return mensagemBoasVindas;
    }

    public void setMensagemBoasVindas(String mensagemBoasVindas) {
        this.mensagemBoasVindas = mensagemBoasVindas;
    }
}
