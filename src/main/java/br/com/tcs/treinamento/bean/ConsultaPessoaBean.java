package br.com.tcs.treinamento.bean;

import br.com.tcs.treinamento.entity.Pessoa;
import br.com.tcs.treinamento.service.PessoaService;
import br.com.tcs.treinamento.service.impl.PessoaServiceImpl;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.primefaces.PrimeFaces;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.itextpdf.text.Document;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.DocumentException;

@ManagedBean(name = "consultaPessoaBean")
@ViewScoped
public class ConsultaPessoaBean implements Serializable {

    private List<Pessoa> pessoas;
    private Pessoa pessoaSelecionada;
    private String errorMessage;
    private Long pessoaId;
    private Boolean tpManutencao;
    private String filtroNome;
    private List<Pessoa> pessoasSelecionadas;


    private transient PessoaService pessoaService = new PessoaServiceImpl();

    @PostConstruct
    public void init() {
        // Recupera parâmetro "pessoaId" da URL
        Map<String, String> params = FacesContext.getCurrentInstance()
                .getExternalContext()
                .getRequestParameterMap();
        String idParam = params.get("pessoaId");
        if (idParam != null && !idParam.trim().isEmpty()) {
            try {
                pessoaId = Long.valueOf(idParam);
                pessoaSelecionada = pessoaService.buscarPorId(pessoaId);
            } catch (NumberFormatException e) {
                errorMessage = "ID inválido da pessoa.";
            }
        }
        // Recupera o parâmetro tpManutencao; se não existir, assume um valor padrão (por exemplo, true para edição)
        String tpParam = params.get("tpManutencao");
        if (tpParam != null && !tpParam.trim().isEmpty()) {
            setTpManutencao(Boolean.valueOf(tpParam));
        } else {
            setTpManutencao(true);
        }
        pessoas = pessoaService.listar();
    }

    public String prepararEdicao(Pessoa pessoa) {
        this.pessoaSelecionada = pessoa;
        return "alterar?faces-redirect=true&pessoaId=" + pessoa.getId() + "&tpManutencao=true";
    }

    public String prepararExclusao(Pessoa pessoa) {
        this.pessoaSelecionada = pessoa;
        return "excluir?faces-redirect=true&pessoaId=" + pessoa.getId() + "&tpManutencao=false";
    }

    public String atualizarConsulta() {
        pessoaService.atualizar(pessoaSelecionada);
        pessoas = pessoaService.listar();
        return "consultaPessoas?faces-redirect=true";
    }

    public void limparAlteracoes() {
        if (pessoaSelecionada != null) {
            pessoaSelecionada = pessoaService.buscarPorId(pessoaSelecionada.getId());
        }
    }

    /**
     * Método que converte o VO para a entidade e chama o service para persistir.
     * Após persistir, exibe o popup de sucesso.
     */
    public void confirmar() {
        // Converte o VO para a entidade Pessoa
        Pessoa pessoa = mapPessoaEntity();
        // Chama o service para persistir a entidade
        try {
            pessoaService.atualizar(pessoa);
            // Exibe o popup de sucesso após a confirmação
            PrimeFaces.current().executeScript("PF('successDialog').show();");
        } catch (Exception e) {
            // Em caso de erro na persistência, exibe o diálogo de erro
            errorMessage = "Erro ao cadastrar pessoa: " + e.getMessage();
            PrimeFaces.current().executeScript("PF('errorDialog').show();");
            return;
        }
    }

    /**
     * mapPessoaEntity
     * Mapeamento da VO para Entity
     */
    private Pessoa mapPessoaEntity() {
        Pessoa pessoa = new Pessoa();
        pessoa.setId(pessoaSelecionada.getId());
        pessoa.setNome(pessoaSelecionada.getNome());
        pessoa.setIdade(pessoaSelecionada.getIdade());
        pessoa.setEmail(pessoaSelecionada.getEmail());
        pessoa.setData(pessoaSelecionada.getData());
        pessoa.setDataCadastro(pessoaSelecionada.getDataCadastro());
        pessoa.setTipoDocumento(pessoaSelecionada.getTipoDocumento());
        pessoa.setNumeroCPF(pessoaSelecionada.getNumeroCPF());
        pessoa.setNumeroCNPJ(pessoaSelecionada.getNumeroCNPJ());
        pessoa.setDataManutencao(new Date());
        pessoa.setAtivo(getTpManutencao());
        return pessoa;
    }

    public void confirmarExclusao(){
        Pessoa pessoa = mapPessoaEntity();
        try {
            pessoaService.atualizar(pessoa); //Exclusao logica
            //pessoaService.excluir(pessoa); // Exclusao fisica
            // Exibe o popup de sucesso após a confirmação
            PrimeFaces.current().executeScript("PF('successDialog').show();");
        } catch (Exception e) {
            // Em caso de erro na persistência, exibe o diálogo de erro
            errorMessage = "Erro ao cadastrar pessoa: " + e.getMessage();
            PrimeFaces.current().executeScript("PF('errorDialog').show();");
            return;
        }
    }
    public void excluirSelecionados() {
        if (pessoasSelecionadas != null && !pessoasSelecionadas.isEmpty()) {
            try {
                for (Pessoa pessoa : pessoasSelecionadas) {
                    // Para exclusão lógica, setamos 'ativo' para false
                    pessoa.setAtivo(false);
                    pessoa.setDataManutencao(new Date()); // Opcional: registrar data da "exclusão"
                    pessoaService.atualizar(pessoa); // Atualiza o status da pessoa para inativo
                }
                pessoasSelecionadas.clear(); // Limpa a lista de selecionados no bean
                pessoas = pessoaService.listar(); // Recarrega a lista principal para atualizar a tabela na UI
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_INFO, "Sucesso", "Pessoas selecionadas excluídas logicamente com sucesso!"));
            } catch (Exception e) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Erro ao excluir pessoas: " + e.getMessage()));
            }
        } else {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "Atenção", "Nenhuma pessoa selecionada para exclusão."));
        }
    }
    public void validarCampos() {
        List<String> erros = new ArrayList<>();

        if (pessoaSelecionada.getNome() == null || pessoaSelecionada.getNome().trim().isEmpty()) {
            erros.add("Nome não informado.");
        }
        if (pessoaSelecionada.getIdade() == null) {
            erros.add("Idade não informada.");
        }
        if (pessoaSelecionada.getEmail() == null || pessoaSelecionada.getEmail().trim().isEmpty()) {
            erros.add("E-mail não informado.");
        }
        if (pessoaSelecionada.getData() == null) {
            erros.add("Data de nascimento não informada.");
        }
        if (pessoaSelecionada.getDataCadastro() == null) {
            erros.add("Data de cadastro não informada");
        }
        if (pessoaSelecionada.getTipoDocumento() == null || pessoaSelecionada.getTipoDocumento().trim().isEmpty()) {
            erros.add("Tipo de documento não informado.");
        } else {
            if ("CPF".equals(pessoaSelecionada.getTipoDocumento())) {
                if (pessoaSelecionada.getNumeroCPF() == null || pessoaSelecionada.getNumeroCPF().trim().isEmpty() ||
                        pessoaSelecionada.getNumeroCPF().trim().length() < 11) {
                    erros.add("CPF não informado ou incompleto (deve conter 11 dígitos).");
                }
            } else if ("CNPJ".equals(pessoaSelecionada.getTipoDocumento())) {
                if (pessoaSelecionada.getNumeroCNPJ() == null || pessoaSelecionada.getNumeroCNPJ().trim().isEmpty() ||
                        pessoaSelecionada.getNumeroCNPJ().trim().length() < 14) {
                    erros.add("CNPJ não informado ou incompleto (deve conter 14 dígitos).");
                }
            }
        }

        if (!erros.isEmpty()) {
            errorMessage = String.join("<br/>", erros);
            PrimeFaces.current().executeScript("PF('errorDialog').show();");
        } else {
            PrimeFaces.current().executeScript("PF('confirmDialog').show();");
        }
    }
    public List<Pessoa> getPessoasFiltradas() {
        if (filtroNome == null || filtroNome.isEmpty()) {
            // Retorna apenas as pessoas ativas se não houver filtro de nome
            return pessoas.stream()
                    .filter(Pessoa::getAtivo)
                    .collect(Collectors.toList());
        }
        // Retorna as pessoas ativas que correspondem ao filtro de nome
        return pessoas.stream()
                .filter(p -> p.getAtivo() && p.getNome().toLowerCase().contains(filtroNome.toLowerCase()))
                .collect(Collectors.toList());
    }

    public void exportarPdf() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) facesContext.getExternalContext().getResponse();

        try {
            Document document = new Document(PageSize.A4);
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment; filename=\"Pessoas.pdf\"");
            PdfWriter.getInstance(document, response.getOutputStream());
            document.open();

            document.add(new Paragraph("Lista de Pessoas"));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(8); // 8 colunas como no Excel
            table.setWidthPercentage(100);

            // Adiciona cabeçalhos
            table.addCell("ID");
            table.addCell("Nome");
            table.addCell("Idade");
            table.addCell("Email");
            table.addCell("Data Nasc.");
            table.addCell("Data Cadastro");
            table.addCell("CPF/CNPJ");
            table.addCell("Status");

            // Popula a tabela com dados
            for (Pessoa p : getPessoasFiltradas()) {
                table.addCell(String.valueOf(p.getId()));
                table.addCell(p.getNome());
                table.addCell(String.valueOf(p.getIdade() != null ? p.getIdade() : ""));
                table.addCell(p.getEmail());
                table.addCell(p.getData() != null ? new java.text.SimpleDateFormat("dd/MM/yyyy").format(p.getData()) : "");
                table.addCell(p.getDataCadastro() != null ? new java.text.SimpleDateFormat("dd/MM/yyyy").format(p.getDataCadastro()) : "");
                table.addCell(p.getTipoDocumento().equals("CPF") ? p.getNumeroCPF() : p.getNumeroCNPJ());
                table.addCell(p.getAtivo() ? "Ativo" : "Inativo");
            }
            document.add(table);
            document.close();
            facesContext.responseComplete();

        } catch (DocumentException | IOException e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Erro ao exportar PDF: " + e.getMessage()));
            e.printStackTrace();
        }
    }

    public void exportarExcel() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) facesContext.getExternalContext().getResponse();

        try (Workbook workbook = new XSSFWorkbook(); // Para formato .xlsx
             ServletOutputStream out = response.getOutputStream()) {

            Sheet sheet = workbook.createSheet("Pessoas");

            Row headerRow = sheet.createRow(0);
            String[] headers = {"ID", "Nome", "Idade", "Email", "Data Nasc.", "Data Cadastro", "CPF/CNPJ", "Status"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            int rowNum = 1;
            for (Pessoa p : getPessoasFiltradas()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(p.getId());
                row.createCell(1).setCellValue(p.getNome());
                row.createCell(2).setCellValue(p.getIdade() != null ? p.getIdade() : 0);
                row.createCell(3).setCellValue(p.getEmail());
                row.createCell(4).setCellValue(p.getData() != null ? new java.text.SimpleDateFormat("dd/MM/yyyy").format(p.getData()) : "");
                row.createCell(5).setCellValue(p.getDataCadastro() != null ? new java.text.SimpleDateFormat("dd/MM/yyyy").format(p.getDataCadastro()) : "");
                row.createCell(6).setCellValue(p.getTipoDocumento().equals("CPF") ? p.getNumeroCPF() : p.getNumeroCNPJ());
                row.createCell(7).setCellValue(p.getAtivo() ? "Ativo" : "Inativo");
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=\"Pessoas.xlsx\"");

            workbook.write(out);
            facesContext.responseComplete(); // Finaliza a resposta do JSF

        } catch (IOException e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Erro ao exportar Excel: " + e.getMessage()));
            e.printStackTrace();
        }
    }

    public List<Pessoa> getPessoas() {
        return pessoas;
    }

    public void setPessoas(List<Pessoa> pessoas) {
        this.pessoas = pessoas;
    }

    public Pessoa getPessoaSelecionada() {
        return pessoaSelecionada;
    }

    public void setPessoaSelecionada(Pessoa pessoaSelecionada) {
        this.pessoaSelecionada = pessoaSelecionada;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Long getPessoaId() {
        return pessoaId;
    }

    public void setPessoaId(Long pessoaId) {
        this.pessoaId = pessoaId;
    }

    public PessoaService getPessoaService() {
        return pessoaService;
    }

    public void setPessoaService(PessoaService pessoaService) {
        this.pessoaService = pessoaService;
    }

    public Boolean getTpManutencao() {
        return tpManutencao;
    }

    public void setTpManutencao(Boolean tpManutencao) {
        this.tpManutencao = tpManutencao;
    }
    public String getFiltroNome() {
        return filtroNome;
    }

    public void setFiltroNome(String filtroNome) {
        this.filtroNome = filtroNome;
    }

    public List<Pessoa> getPessoasSelecionadas() {
        return pessoasSelecionadas;
    }
    public void setPessoasSelecionadas(List<Pessoa> pessoasSelecionadas) {
        this.pessoasSelecionadas = pessoasSelecionadas;
    }
}