package com.mywallet.transaction.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler({ValidationException.class, MethodArgumentTypeMismatchException.class})
	public ProblemDetail validation(HttpServletRequest request) {
		return problem(HttpStatus.BAD_REQUEST, "Requête invalide", "La requête est invalide.", "VALIDATION_ERROR", request);
	}

	@ExceptionHandler({MalformedRequestException.class, HttpMessageNotReadableException.class, HttpMediaTypeNotSupportedException.class})
	public ProblemDetail malformed(HttpServletRequest request) {
		return problem(HttpStatus.BAD_REQUEST, "Requête mal formée", "Le corps de la requête est illisible.", "MALFORMED_REQUEST", request);
	}

	@ExceptionHandler(InvalidAmountException.class)
	public ProblemDetail invalidAmount(HttpServletRequest request) {
		return problem(HttpStatus.UNPROCESSABLE_CONTENT, "Montant invalide", "Le montant est invalide.", "INVALID_AMOUNT", request);
	}

	@ExceptionHandler(UnauthenticatedException.class)
	public ResponseEntity<ProblemDetail> unauthenticated(HttpServletRequest request) {
		ProblemDetail body = problem(
				HttpStatus.UNAUTHORIZED,
				"Non autorisé",
				"Authentification requise.",
				"UNAUTHENTICATED",
				request);
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.header(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
				.contentType(MediaType.APPLICATION_PROBLEM_JSON)
				.body(body);
	}

	@ExceptionHandler(WalletNotFoundException.class)
	public ProblemDetail walletNotFound(HttpServletRequest request) {
		return problem(HttpStatus.NOT_FOUND, "Introuvable", "Wallet introuvable.", "WALLET_NOT_FOUND", request);
	}

	@ExceptionHandler(WalletNotActiveException.class)
	public ProblemDetail walletNotActive(HttpServletRequest request) {
		return problem(HttpStatus.CONFLICT, "Conflit", "Le wallet n'est pas actif.", "WALLET_NOT_ACTIVE", request);
	}

	@ExceptionHandler(RecipientNotFoundException.class)
	public ProblemDetail recipientNotFound(HttpServletRequest request) {
		return problem(
				HttpStatus.NOT_FOUND,
				"Introuvable",
				"Aucun destinataire trouvé pour ce numéro.",
				"RECIPIENT_NOT_FOUND",
				request);
	}

	@ExceptionHandler(SelfTransferException.class)
	public ProblemDetail selfTransfer(HttpServletRequest request) {
		return problem(
				HttpStatus.BAD_REQUEST,
				"Requête invalide",
				"Vous ne pouvez pas vous envoyer de l’argent à vous-même.",
				"SELF_TRANSFER",
				request);
	}

	@ExceptionHandler(RecipientWalletNotActiveException.class)
	public ProblemDetail recipientWalletNotActive(HttpServletRequest request) {
		return problem(
				HttpStatus.CONFLICT,
				"Conflit",
				"Le portefeuille du destinataire n’est pas actif.",
				"RECIPIENT_WALLET_NOT_ACTIVE",
				request);
	}

	@ExceptionHandler(IdempotencyConflictException.class)
	public ProblemDetail idempotency(HttpServletRequest request) {
		return problem(
				HttpStatus.CONFLICT,
				"Conflit",
				"Cette clé d'idempotence est déjà utilisée.",
				"IDEMPOTENCY_CONFLICT",
				request);
	}

	@ExceptionHandler(TransactionNotFoundException.class)
	public ProblemDetail notFound(HttpServletRequest request) {
		return problem(HttpStatus.NOT_FOUND, "Introuvable", "Transaction introuvable.", "TRANSACTION_NOT_FOUND", request);
	}

	@ExceptionHandler(ServiceUnavailableException.class)
	public ProblemDetail unavailable(HttpServletRequest request) {
		return problem(
				HttpStatus.SERVICE_UNAVAILABLE,
				"Service indisponible",
				"Le wallet est indisponible.",
				"SERVICE_UNAVAILABLE",
				request);
	}

	@ExceptionHandler(Exception.class)
	public ProblemDetail unexpected(Exception exception, HttpServletRequest request) {
		log.error("Erreur interne inattendue ({})", exception.getClass().getSimpleName());
		return problem(
				HttpStatus.INTERNAL_SERVER_ERROR,
				"Erreur interne",
				"Une erreur interne est survenue.",
				"INTERNAL_ERROR",
				request);
	}

	private static ProblemDetail problem(
			HttpStatus status,
			String title,
			String detail,
			String code,
			HttpServletRequest request) {
		return ProblemDetails.of(status, title, detail, code, request);
	}

}
