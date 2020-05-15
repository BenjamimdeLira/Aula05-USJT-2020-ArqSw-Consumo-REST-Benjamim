package br.usjt.usjt_ccp3_consumo_img_init.model.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;
import javax.servlet.ServletContext;
import javax.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import br.usjt.usjt_ccp3_consumo_img_init.model.dao.FilmeDAO;
import br.usjt.usjt_ccp3_consumo_img_init.model.dao.local;
import br.usjt.usjt_ccp3_consumo_img_init.model.entity.Filme;
import br.usjt.usjt_ccp3_consumo_img_init.model.entity.Genero;
import net.bytebuddy.asm.Advice.Local;
import usjt_ccp3_consumo_img_init.model.javabeans.Creditos;
import usjt_ccp3_consumo_img_init.model.javabeans.Equipe;
import usjt_ccp3_consumo_img_init.model.javabeans.Lancamentos;
import usjt_ccp3_consumo_img_init.model.javabeans.Movie;
import usjt_ccp3_consumo_img_init.model.javabeans.Populares;

@Service
public class FilmeService {

	private static final String API_KEY = "144abacc9a41ad16273617d9249dd9b2";
	private static final String BASE_URL = "https://api.themoviedb.org/3/movie";
	private static final String POPULAR = "/popular?api_key" + API_KEY + "&language=pt+BR&page=1&region=BR";
	private static final String UPCOMING = "/upcoming?api_key" + API_KEY + "&language=pt+BR&page=1&region=BR";
	private static final String POSTER_URL = "https://image.tmdb.org/t/p/u400";
	private static final String CREDITS = "/credits?api_key=" + API_KEY;
	private static final int Overview = 0;

	@Autowired
	private FilmeDAO dao;

	public Filme buscarFilme(int id) throws IOException {
		return dao.buscarFilme(id);
	}

	@Transactional
	public Filme inserirFilme(Filme filme) throws IOException {
		int id = dao.inserirFilme(filme);
		filme.setId(id);
		return filme;
		/*
		 * @RequestParam("file") MultipartFile (file) { ls.gravarImagem(servletContext,
		 * filme, file); return "redirect:listar_filmes";
		 */
	}

	@Transactional
	public void atualizarFilme(Filme filme) throws IOException {
		dao.atualizarFilme(filme);
	}

	@Transactional
	public void excluirFilme(int id) throws IOException {
		dao.excluirFilme(id);
	}

	public List<Filme> listarFilmes(String chave) throws IOException {
		return dao.listarFilmes(chave);
	}

	public List<Filme> listarFilmes() throws IOException {
		return dao.listarFilmes();
	}

	@Transactional
	public Filme baixarFilmesMaisPopulares() throws IOException {
		RestTemplate rest = new RestTemplate();
		Populares resultado = rest.getForObject(BASE_URL + POPULAR, Populares.class);
		System.out.println("Resultado: " + resultado);

		for (Movie movie : resultado.getResults()) {
			Filme filme = toFilme(movie);
			dao.buscarFilme1(filme.getDescricao());
			boolean exists = false;
			if (exists)
				return Movie.get(Overview);
			else
				dao.inserirFilme(filme);
		}
		return null;
	}

	@Transactional
	public void baixarLancamentos() throws IOException {
		RestTemplate rest = new RestTemplate();
		Lancamentos resultado = rest.getForObject(BASE_URL + UPCOMING, Lancamentos.class);
		System.out.println("Resultado: " + resultado);
		for (Movie movie : resultado.getResults()) {
			Filme filme = toFilme(movie);
			dao.inserirFilme(filme);
		}
	}

	private Filme toFilme(Movie movie) {
		RestTemplate rest = new RestTemplate();
		Creditos resultado = rest.getForObject(BASE_URL + "/" + movie + getID() + CREDITS, Creditos.class);
		System.out.println("Diretor: " + resultado);
		Filme filme = new Filme();
		int count = 0;
		for (Equipe equipe : resultado.getCrew()) {
			if (equipe.getJob().equals("Director")) {
				count++;
				if (count == 1) {
					filme.setDiretor(equipe.getName());
				} else {
					filme.setDiretor(filme.getDiretor() + ": " + equipe.getName());
				}
			}
		}
		filme.setDataLancamento(movie.getRelease_date());
		filme.setDescricao(movie.getOverview());
		Genero genero = new Genero();
		try {
			genero.setId(movie.getGenre_ids()[0]);
		} catch (Exception e) {
			genero.setId(1);
		}
		filme.setGenero(genero);
		filme.setPopularidade(movie.getPopularity());
		filme.setPosterPath(POSTER_URL + movie.getPoster_path());
		filme.setTitulo(movie.getTitle());
		return filme;
	}

	private String getID() {
		// TODO Auto-generated method stub
		return null;
	}

	public void gravarImagem(Object servletContext, Local local, MultipartFile file) throws IOException {
		MultipartFile lfile;
		if (lfile.isEmpty()) {
			BufferedImage src = ImageIO.read(new ByteArrayInputStream(file.getBytes()));
			String path = ((ServletContext) servletContext).getRealPath(((ServletContext) servletContext).getContextPath());
			path = path.substring(0, path.lastIndexOf('/'));
			String nomeArquivo = "img" + ((local) local).getId() + " .jpg";
			((br.usjt.usjt_ccp3_consumo_img_init.model.dao.local) local).setImagem(nomeArquivo);
			atualizar(local, null, lfile);
			File destination = new File(path + file.separatorChar + "img" + File.pathSeparatorChar + nomeArquivo);
			if (destination.exists()) {
				destination.delete();
			}
			ImageIO.write(src, "jpg", destination);
		}
	}
	
	@RequestMapping("atualizar_local")
	public String atualizar(Local local, Model model, @RequestParam("file") MultipartFile file) {
		try {
			Object ls;
			((Object) ls).atualizar(local);
			Object servletContext;
			((FilmeService) ls).gravarImagem(servletContext, local, file);
			return "redirect:listar_locais";
			
		} catch (IOException e) {
			e.printStackTrace();
			model.addAttribute("erro", e);
		}
		return "erro";
	}
}
